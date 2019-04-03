package com.wacai.open.baige.common.util;


import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public final class SignUtil {

  public static String generateSign(String plainText, String cipher) {
    Mac mac;
    String algorithm = "hmacSha256";
    try {
      mac = Mac.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(algorithm, e);
    }
    byte[] secretBytes = cipher.getBytes(StandardCharsets.UTF_8);
    try {
      mac.init(new SecretKeySpec(secretBytes, algorithm));
    } catch (InvalidKeyException e) {
      throw new RuntimeException("cipher : " + cipher, e);
    }
    byte[] signatureBytes = mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    return Base64.encodeBase64URLSafeString(signatureBytes);
  }


}

