package com.healthvia.platform.booking;

import com.healthvia.platform.config.IyzicoProperties;
import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreatePaymentRequest;

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

    public record PaymentResult(boolean success, String paymentId, String errorMessage) {}
}
