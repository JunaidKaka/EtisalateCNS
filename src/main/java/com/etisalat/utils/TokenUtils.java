package com.etisalat.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenUtils {
    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateRandomToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
