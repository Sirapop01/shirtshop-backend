package com.shirtshop.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

public final class PromptPayQr {

    private PromptPayQr() {}

    /* ---------- EMV TLV helpers ---------- */
    private static String tlv(String id, String value) {
        String len = String.format("%02d", value.length());
        return id + len + value;
    }

    private static String crc16(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.US_ASCII);
        int polynomial = 0x1021;
        int reg = 0xFFFF;
        for (byte b : bytes) {
            reg ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((reg & 0x8000) != 0) reg = (reg << 1) ^ polynomial;
                else reg <<= 1;
                reg &= 0xFFFF;
            }
        }
        return String.format("%04X", reg & 0xFFFF);
    }

    private static boolean isPhone(String digits) {
        // ไทย: 10 หลักขึ้นต้น 0 เช่น 0812345678
        return digits.matches("^0\\d{9}$");
    }

    // ทำให้เป็น ASCII A-Z0-9 และตัดความยาวไม่เกิน 25 ตัว (เพื่อความเข้ากันได้ของบางแอป)
    private static String sanitizeAsciiUpper(String in, int maxLen) {
        String s = in == null ? "" : in.replaceAll("[^\\x20-\\x7E]", "");
        s = s.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) s = "NA";
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    /** สร้าง EMV payload สำหรับ PromptPay (มี amount) */
    public static String buildEmvPayload(String targetRaw, BigDecimal amountBaht, boolean dynamic) {
        String digits = targetRaw.replaceAll("\\D", "");

        // 00: Payload Format Indicator
        String pfi = tlv("00", "01");
        // 01: Point of Initiation Method (11=Static, 12=Dynamic)
        String poi = tlv("01", dynamic ? "12" : "11");

        // 29: Merchant Account Information (PromptPay)
        //   00: AID (ต้องเป็นค่านี้เสมอ)
        //   01: เบอร์โทร 0066xxxxxxxxx (ตัด 0 ตัวหน้า)
        //   02: เลขบัตรประชาชน 13 หลัก
        //   03: e-Wallet ID (ถ้ามี)
        String aid = tlv("00", "A000000677010111");

        String sub;
        if (isPhone(digits)) {
            String international = "0066" + digits.substring(1); // 0066 + ตัด 0 ข้างหน้า
            sub = aid + tlv("01", international);
        } else if (digits.matches("^\\d{13}$")) {
            sub = aid + tlv("02", digits);
        } else {
            sub = aid + tlv("03", digits);
        }
        String mai = tlv("29", sub);

        // 53: Currency (THB=764)
        String currency = tlv("53", "764");

        // 54: Amount (สองตำแหน่งทศนิยม ใช้จุด)
        String amt = tlv("54", amountBaht.setScale(2).toPlainString());

        // 58: Country Code, 59: Merchant Name, 60: City
        String country = tlv("58", "TH");
        String name = tlv("59", sanitizeAsciiUpper("StyleWhere", 25));
        String city = tlv("60", sanitizeAsciiUpper("Bangkok", 15));

        // รวมก่อนคำนวณ CRC (ต้องเติม "6304" ก่อนหา CRC)
        String withoutCrc = pfi + poi + mai + currency + amt + country + name + city + "6304";
        String crc = crc16(withoutCrc);
        return withoutCrc + crc;
    }

    public static String toPngDataUrl(String payload, int size) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, size, size);
            BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            throw new RuntimeException("Failed generating QR", e);
        }
    }

    /** Shortcut: target (เบอร์/บัตร) + amount(บาท) -> data URL PNG */
    public static String generatePromptPayQrDataUrl(String targetRaw, int totalBaht, int size, boolean dynamic) {
        String amount = String.format(Locale.US, "%.2f", totalBaht * 1.0);
        String payload = buildEmvPayload(targetRaw, new BigDecimal(amount), dynamic);
        return toPngDataUrl(payload, size);
    }
}
