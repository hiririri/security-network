package com.dauphine.security.tp4;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

class CMAC {
    SecretKey key;
    byte[] k1, k2;
    static byte[] irrPoly = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x87};

    public static byte[] gfMult(byte[] toMult) {
        byte[] res = new byte[irrPoly.length];
        boolean isGood = false;
        for (int i = toMult.length - 1; i >= 0; i--) {
            res[i] = (byte) (toMult[i] << 1);
            if (isGood) {
                res[i] |= 1;
                isGood = false;
            }
            if (toMult[i] > 0) {
                isGood = true;
            }
        }
        if (isGood) {
            res[irrPoly.length - 1] ^= irrPoly[irrPoly.length - 1];
        }
        return res;
    }

    public CMAC(SecretKey key) throws Exception {
        this.key = key;
        byte[] K1 = new byte[16];
        byte[] K2 = new byte[16];
        byte[] zero = new byte[16];

        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        K1 = gfMult(cipher.doFinal(zero));
        K2 = gfMult(K1);

        this.k1 = K1;
        this.k2 = K2;
    }

    public byte[] authentify(byte[] message) throws Exception {
        byte[] res = new byte[16];
        byte[] iIterationsMessage = new byte[16];

        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        int iterations = message.length / 16;
        for (int i = 0; i < iterations; i++) {
            for (int j = 0; j < 16; j++) {
                iIterationsMessage[j] = message[i * 16 + j];
            }
            if (i != 0) {
                for (int j = 0; j < 16; j++) {
                    iIterationsMessage[j] = (byte) (message[i * 16 + j] ^ res[j]);
                }
            }
            for (int j = 0; j < 16; j++) {
                res[j] = cipher.doFinal(iIterationsMessage)[j];
            }
        }

        if (message.length % 16 == 0) {
            for (int j = 0; j < 16; j++) {
                byte b = iIterationsMessage[(iterations - 1) * 16 + j];
                byte b1 = k1[j];
                byte b2 = res[j];
                res[j] = (byte) (b ^ b1 ^ b2);
            }

        } else {
            int difference = message.length - iterations * 16;
            byte[] paddingMessage = new byte[16];
            for (int i = 0; i < difference; i++) {
                paddingMessage[i] =
                        message[(iterations - 1) * 16 + i];
            }
            paddingMessage[difference + 1] = (byte) 0x80;
            for (int i = difference + 2; i < 16 - difference - 1; i++) {
                paddingMessage[i] = 0;
            }

            for (int j = iterations * 16; j < message.length; j++) {
                res[j] = (byte) (paddingMessage[iterations * 16 + j] ^ k2[j] ^ res[j]);
            }
        }

        return res;
    }

    public static void main(String[] args) {
        String key = "2B7E151628AED2A6ABF7158809CF4F3C";
        byte[] keyBytes = CCM.hexStringToByteArray(key);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
        String key2 = "404142434445464748494A4B4C4D4E4F";
        byte[] keyBytes2 = CCM.hexStringToByteArray(key2);
        SecretKey secretKey2 = new SecretKeySpec(keyBytes2, "AES");
        try {
            CMAC cmac = new CMAC(secretKey);
            byte[] messageByte = new byte[16];
            byte[] macValue = cmac.authentify(messageByte);
            System.out.println(byteArrayToHexString(macValue));
            String message = "6BC1BEE22E409F96E93D7E117393172A";
            messageByte = CCM.hexStringToByteArray(message);
            macValue = cmac.authentify(messageByte);
            System.out.println(byteArrayToHexString(macValue));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String byteArrayToHexString(byte[] byteArray) { //generated by ChatGPT
        StringBuilder hexString = new StringBuilder();
        for (byte b : byteArray) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }
}