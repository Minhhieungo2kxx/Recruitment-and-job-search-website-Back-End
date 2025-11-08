package com.webjob.application.Util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Util {
    public static String encode(String plain) {
        return Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
