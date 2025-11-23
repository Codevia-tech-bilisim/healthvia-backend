package com.healthvia.platform.common.util;

/**
 * TC Kimlik Numarası doğrulama utility sınıfı
 * TC Kimlik No algoritması:
 * - 11 haneli olmalı
 * - İlk hane 0 olamaz
 * - İlk 9 hanenin toplamının 10'a bölümünden kalan 10. haneyi verir
 * - İlk 10 hanenin toplamının 10'a bölümünden kalan 11. haneyi verir
 * - 1,3,5,7,9 hanelerin toplamının 7 katından 2,4,6,8 hanelerin toplamı çıkarıldığında
 *   10'a bölümünden kalan 10. haneyi verir
 */
public class TcKimlikNoValidator {

    /**
     * TC Kimlik numarasının geçerli olup olmadığını kontrol eder
     * @param tcKimlikNo kontrol edilecek TC Kimlik numarası
     * @return geçerli ise true, değilse false
     */
    public static boolean isValid(String tcKimlikNo) {
        // Null veya boş kontrol
        if (tcKimlikNo == null || tcKimlikNo.trim().isEmpty()) {
            return false;
        }

        // Boşlukları temizle
        tcKimlikNo = tcKimlikNo.trim();

        // 11 haneli olmalı
        if (tcKimlikNo.length() != 11) {
            return false;
        }

        // Sadece rakamlardan oluşmalı
        if (!tcKimlikNo.matches("\\d{11}")) {
            return false;
        }

        // İlk hane 0 olamaz
        if (tcKimlikNo.charAt(0) == '0') {
            return false;
        }

        // Algoritma kontrolü
        try {
            int[] digits = new int[11];
            for (int i = 0; i < 11; i++) {
                digits[i] = Character.getNumericValue(tcKimlikNo.charAt(i));
            }

            // 10. hane kontrolü
            // 1,3,5,7,9 hanelerin toplamının 7 katından 2,4,6,8 hanelerin toplamı çıkarılır
            int oddSum = digits[0] + digits[2] + digits[4] + digits[6] + digits[8];
            int evenSum = digits[1] + digits[3] + digits[5] + digits[7];
            int digit10 = ((oddSum * 7) - evenSum) % 10;
            
            if (digit10 != digits[9]) {
                return false;
            }

            // 11. hane kontrolü
            // İlk 10 hanenin toplamının 10'a bölümünden kalan
            int sumFirst10 = 0;
            for (int i = 0; i < 10; i++) {
                sumFirst10 += digits[i];
            }
            int digit11 = sumFirst10 % 10;
            
            return digit11 == digits[10];

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * TC Kimlik numarasını formatlar (sadece rakamları alır)
     * @param tcKimlikNo formatlanacak TC Kimlik numarası
     * @return formatlanmış TC Kimlik numarası
     */
    public static String format(String tcKimlikNo) {
        if (tcKimlikNo == null) {
            return null;
        }
        // Sadece rakamları al
        return tcKimlikNo.replaceAll("[^0-9]", "");
    }

    /**
     * TC Kimlik numarasını maskeler (gösterim için)
     * Örnek: 12345678901 -> 123****8901
     * @param tcKimlikNo maskelenecek TC Kimlik numarası
     * @return maskelenmiş TC Kimlik numarası
     */
    public static String mask(String tcKimlikNo) {
        if (!isValid(tcKimlikNo)) {
            return tcKimlikNo;
        }
        return tcKimlikNo.substring(0, 3) + "****" + tcKimlikNo.substring(7);
    }

    /**
     * Test için örnek geçerli TC Kimlik numaraları üretir
     * NOT: Bu metod sadece test amaçlıdır, production'da kullanılmamalıdır
     */
    public static String generateTestTc() {
        // İlk 9 hane için random sayılar
        int[] digits = new int[11];
        digits[0] = (int) (Math.random() * 9) + 1; // İlk hane 1-9 arası
        
        for (int i = 1; i < 9; i++) {
            digits[i] = (int) (Math.random() * 10);
        }
        
        // 10. haneyi hesapla
        int oddSum = digits[0] + digits[2] + digits[4] + digits[6] + digits[8];
        int evenSum = digits[1] + digits[3] + digits[5] + digits[7];
        int digit10Raw = ((oddSum * 7) - evenSum) % 10;
        // Java'da negatif sayilarin modulo'su negatif olabilir, pozitif yapalim
        digits[9] = digit10Raw < 0 ? digit10Raw + 10 : digit10Raw;
        
        // 11. haneyi hesapla
        int sumFirst10 = 0;
        for (int i = 0; i < 10; i++) {
            sumFirst10 += digits[i];
        }
        digits[10] = sumFirst10 % 10;
        
        // String'e çevir
        StringBuilder sb = new StringBuilder();
        for (int digit : digits) {
            sb.append(digit);
        }
        return sb.toString();
    }
}