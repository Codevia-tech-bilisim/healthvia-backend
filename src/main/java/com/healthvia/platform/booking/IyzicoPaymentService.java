package com.healthvia.platform.booking;

import com.healthvia.platform.config.IyzicoProperties;
import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreateCancelRequest;
import com.iyzipay.request.CreateCheckoutFormInitializeRequest;
import com.iyzipay.request.CreatePaymentRequest;
import com.iyzipay.request.RetrieveCheckoutFormRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IyzicoPaymentService {

    private final IyzicoProperties iyzicoProperties;

    private Options getOptions() {
        Options options = new Options();
        options.setApiKey(iyzicoProperties.getApiKey());
        options.setSecretKey(iyzicoProperties.getSecretKey());
        options.setBaseUrl(iyzicoProperties.getBaseUrl());
        return options;
    }

    /**
     * Process payment via iyzico API.
     *
     * @param request   booking request with card details
     * @param buyerName full name (first + last)
     * @param buyerEmail buyer email
     * @param buyerId   buyer user ID
     * @param totalPrice total amount to charge
     * @param appointmentId used as conversation/basket ID
     * @param ip         buyer IP address
     * @return PaymentResult with success/failure info
     */
    public PaymentResult processPayment(BookingRequest request,
                                         String buyerName,
                                         String buyerEmail,
                                         String buyerId,
                                         String buyerPhone,
                                         BigDecimal totalPrice,
                                         String appointmentId,
                                         String ip) {

        try {
            CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
            paymentRequest.setLocale(Locale.TR.getValue());
            paymentRequest.setConversationId(appointmentId);
            paymentRequest.setPrice(totalPrice);
            paymentRequest.setPaidPrice(totalPrice);
            paymentRequest.setCurrency(Currency.TRY.name());
            paymentRequest.setInstallment(1);
            paymentRequest.setBasketId("HEALTHVIA-" + appointmentId);
            paymentRequest.setPaymentChannel(PaymentChannel.WEB.name());
            paymentRequest.setPaymentGroup(PaymentGroup.PRODUCT.name());

            // Card details
            PaymentCard paymentCard = new PaymentCard();
            paymentCard.setCardHolderName(request.getCardHolderName());
            paymentCard.setCardNumber(request.getCardNumber());
            paymentCard.setExpireMonth(request.getExpireMonth());
            paymentCard.setExpireYear(request.getExpireYear());
            paymentCard.setCvc(request.getCvc());
            paymentCard.setRegisterCard(0);
            paymentRequest.setPaymentCard(paymentCard);

            // Buyer info
            String[] nameParts = buyerName.split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : firstName;

            Buyer buyer = new Buyer();
            buyer.setId(buyerId);
            buyer.setName(firstName);
            buyer.setSurname(lastName);
            buyer.setEmail(buyerEmail);
            buyer.setIdentityNumber("74300864791");
            buyer.setGsmNumber(buyerPhone != null && !buyerPhone.isEmpty() ? buyerPhone : "+905350000000");
            buyer.setRegistrationAddress("Istanbul, Turkey");
            buyer.setIp(ip != null ? ip : "85.34.78.112");
            buyer.setCity("Istanbul");
            buyer.setCountry("Turkey");
            paymentRequest.setBuyer(buyer);

            // Addresses (required by iyzico)
            Address address = new Address();
            address.setContactName(buyerName);
            address.setCity("Istanbul");
            address.setCountry("Turkey");
            address.setAddress("Istanbul, Turkey");
            paymentRequest.setShippingAddress(address);
            paymentRequest.setBillingAddress(address);

            // Basket items
            List<BasketItem> basketItems = new ArrayList<>();
            BasketItem basketItem = new BasketItem();
            basketItem.setId("APPT-" + appointmentId);
            basketItem.setName("Medical Consultation");
            basketItem.setCategory1("Health");
            basketItem.setCategory2("Consultation");
            basketItem.setItemType(BasketItemType.VIRTUAL.name());
            basketItem.setPrice(totalPrice);
            basketItems.add(basketItem);
            paymentRequest.setBasketItems(basketItems);

            log.info("Processing iyzico payment for appointment: {}, amount: {} TRY", appointmentId, totalPrice);

            Payment payment = Payment.create(paymentRequest, getOptions());

            if ("success".equalsIgnoreCase(payment.getStatus())) {
                log.info("Payment successful for appointment: {}, paymentId: {}", appointmentId, payment.getPaymentId());
                return new PaymentResult(true, payment.getPaymentId(), null);
            } else {
                String errorMsg = payment.getErrorMessage() != null
                        ? payment.getErrorMessage()
                        : "Payment failed";
                log.warn("Payment failed for appointment: {}, error: {}", appointmentId, errorMsg);
                return new PaymentResult(false, null, errorMsg);
            }
        } catch (Exception e) {
            log.error("Payment processing error for appointment: {}", appointmentId, e);
            return new PaymentResult(false, null, "Payment processing error: " + e.getMessage());
        }
    }

    /**
     * Cancel/refund a payment via iyzico API.
     */
    public PaymentResult refundPayment(String paymentId, String conversationId) {
        try {
            CreateCancelRequest cancelRequest = new CreateCancelRequest();
            cancelRequest.setLocale(Locale.TR.getValue());
            cancelRequest.setConversationId(conversationId);
            cancelRequest.setPaymentId(paymentId);

            log.info("Processing iyzico refund for paymentId: {}", paymentId);

            Cancel cancel = Cancel.create(cancelRequest, getOptions());

            if ("success".equalsIgnoreCase(cancel.getStatus())) {
                log.info("Refund successful for paymentId: {}", paymentId);
                return new PaymentResult(true, paymentId, null);
            } else {
                String errorMsg = cancel.getErrorMessage() != null
                        ? cancel.getErrorMessage()
                        : "Refund failed";
                log.warn("Refund failed for paymentId: {}, error: {}", paymentId, errorMsg);
                return new PaymentResult(false, paymentId, errorMsg);
            }
        } catch (Exception e) {
            log.error("Refund processing error for paymentId: {}", paymentId, e);
            return new PaymentResult(false, paymentId, "Refund processing error: " + e.getMessage());
        }
    }

    /**
     * Direct charge via card details — used by agent-assisted flow. Card data
     * never leaves the request object (forwarded to iyzico, not persisted).
     */
    public PaymentResult chargeWithCard(
            String conversationId,
            String basketLabel,
            BigDecimal amount,
            String currency,
            String cardHolderName,
            String cardNumber,
            String expireMonth,
            String expireYear,
            String cvc,
            String buyerName,
            String buyerEmail,
            String buyerId,
            String buyerPhone,
            String ip) {

        BookingRequest req = new BookingRequest();
        req.setAppointmentId(conversationId);
        req.setCardHolderName(cardHolderName);
        req.setCardNumber(cardNumber);
        req.setExpireMonth(expireMonth);
        req.setExpireYear(expireYear);
        req.setCvc(cvc);
        // Note: existing processPayment is hardcoded to TRY; sandbox accepts that.
        // Multi-currency flows can call iyzico directly when needed.
        return processPayment(req, buyerName, buyerEmail, buyerId, buyerPhone, amount,
                conversationId, ip);
    }

    /**
     * Initialise iyzico hosted Checkout Form. Returns a paymentPageUrl that
     * the agent shares with the patient; the patient pays on iyzico's
     * sandbox/production page (PCI scope stays with iyzico).
     */
    public CheckoutInitResult initializeCheckoutForm(
            String conversationId,
            String basketLabel,
            BigDecimal amount,
            String currency,
            String buyerName,
            String buyerEmail,
            String buyerId,
            String buyerPhone,
            String ip,
            String callbackUrl) {

        try {
            CreateCheckoutFormInitializeRequest req = new CreateCheckoutFormInitializeRequest();
            req.setLocale(Locale.TR.getValue());
            req.setConversationId(conversationId);
            req.setPrice(amount);
            req.setPaidPrice(amount);
            // iyzico sandbox API requires TRY for test cards in many configurations;
            // use provided currency, falling back to TRY if blank.
            req.setCurrency((currency == null || currency.isBlank()) ? Currency.TRY.name() : currency);
            req.setBasketId("HEALTHVIA-" + conversationId);
            req.setPaymentGroup(PaymentGroup.PRODUCT.name());
            req.setCallbackUrl(callbackUrl);
            req.setEnabledInstallments(new ArrayList<>(List.of(2, 3, 6, 9)));

            String[] nameParts = (buyerName == null ? "Hasta" : buyerName).split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : firstName;

            Buyer buyer = new Buyer();
            buyer.setId(buyerId == null ? "guest-" + conversationId : buyerId);
            buyer.setName(firstName);
            buyer.setSurname(lastName);
            buyer.setEmail(buyerEmail == null ? "patient@healthvia.com" : buyerEmail);
            buyer.setIdentityNumber("74300864791");
            buyer.setGsmNumber(buyerPhone == null || buyerPhone.isEmpty() ? "+905350000000" : buyerPhone);
            buyer.setRegistrationAddress("Istanbul, Turkey");
            buyer.setIp(ip == null ? "85.34.78.112" : ip);
            buyer.setCity("Istanbul");
            buyer.setCountry("Turkey");
            req.setBuyer(buyer);

            Address address = new Address();
            address.setContactName(buyerName == null ? "Hasta" : buyerName);
            address.setCity("Istanbul");
            address.setCountry("Turkey");
            address.setAddress("Istanbul, Turkey");
            req.setShippingAddress(address);
            req.setBillingAddress(address);

            List<BasketItem> basket = new ArrayList<>();
            BasketItem item = new BasketItem();
            item.setId("HV-" + conversationId);
            item.setName(basketLabel == null ? "HealthVia Treatment Package" : basketLabel);
            item.setCategory1("Health");
            item.setCategory2("Medical Tourism");
            item.setItemType(BasketItemType.VIRTUAL.name());
            item.setPrice(amount);
            basket.add(item);
            req.setBasketItems(basket);

            log.info("Initialising iyzico Checkout Form for {} ({} {})", conversationId, amount, currency);
            CheckoutFormInitialize init = CheckoutFormInitialize.create(req, getOptions());

            if ("success".equalsIgnoreCase(init.getStatus())) {
                return new CheckoutInitResult(true, init.getToken(), init.getPaymentPageUrl(), null);
            }
            String err = init.getErrorMessage() == null ? "Checkout init failed" : init.getErrorMessage();
            log.warn("Iyzico checkout init failed: {}", err);
            return new CheckoutInitResult(false, null, null, err);
        } catch (Exception e) {
            log.error("Iyzico checkout init exception", e);
            return new CheckoutInitResult(false, null, null, e.getMessage());
        }
    }

    /**
     * Retrieve the result of a previously initialised CheckoutForm. Called
     * from the iyzico callback handler to mark our PaymentRequest as PAID.
     */
    public CheckoutRetrieveResult retrieveCheckoutForm(String iyzicoToken, String conversationId) {
        try {
            RetrieveCheckoutFormRequest req = new RetrieveCheckoutFormRequest();
            req.setLocale(Locale.TR.getValue());
            req.setConversationId(conversationId);
            req.setToken(iyzicoToken);
            CheckoutForm result = CheckoutForm.retrieve(req, getOptions());
            boolean paid = "success".equalsIgnoreCase(result.getStatus())
                && "SUCCESS".equalsIgnoreCase(result.getPaymentStatus());
            return new CheckoutRetrieveResult(paid, result.getPaymentId(),
                paid ? null : (result.getErrorMessage() == null ? result.getPaymentStatus() : result.getErrorMessage()));
        } catch (Exception e) {
            log.error("Iyzico retrieve exception", e);
            return new CheckoutRetrieveResult(false, null, e.getMessage());
        }
    }

    public record PaymentResult(boolean success, String paymentId, String errorMessage) {}
    public record CheckoutInitResult(boolean success, String token, String paymentPageUrl, String errorMessage) {}
    public record CheckoutRetrieveResult(boolean paid, String paymentId, String errorMessage) {}
}
