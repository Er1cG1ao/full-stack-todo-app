package com.example.todobackend.user;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** 256-bit random keys are shown once; only a SHA-256 digest is persisted. */
public final class RecoveryKeys {
  private static final SecureRandom RANDOM = new SecureRandom();

  private RecoveryKeys() {}

  public static String generate() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return "DAYLIGHT-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String hash(String key) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static boolean matches(String key, String hash) {
    return key != null
        && key.matches("DAYLIGHT-[A-Za-z0-9_-]{43}")
        && hash != null
        && MessageDigest.isEqual(
            hash(key).getBytes(StandardCharsets.US_ASCII),
            hash.getBytes(StandardCharsets.US_ASCII));
  }
}
