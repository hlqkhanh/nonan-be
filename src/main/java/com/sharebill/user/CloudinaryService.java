package com.sharebill.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CloudinaryService {
  private static final String FOLDER_AVATARS = "sharebill/avatars";
  private static final String FOLDER_BILLS = "sharebill/bills";
  private static final java.util.Set<String> ALLOWED_FOLDERS = java.util.Set.of(FOLDER_AVATARS, FOLDER_BILLS);

  @Value("${cloudinary.cloud-name}")
  private String cloudName;

  @Value("${cloudinary.api-key}")
  private String apiKey;

  @Value("${cloudinary.api-secret}")
  private String apiSecret;

  public AvatarSignatureResponse createUploadSignature() {
    return createUploadSignature(FOLDER_AVATARS);
  }

  /** Issues a signed-upload signature scoped to an allowlisted folder (avatars or bill photos). */
  public AvatarSignatureResponse createUploadSignature(String folder) {
    if (!ALLOWED_FOLDERS.contains(folder)) {
      throw new IllegalArgumentException("Unsupported upload folder: " + folder);
    }
    long timestamp = Instant.now().getEpochSecond();
    String paramsToSign = "folder=" + folder + "&timestamp=" + timestamp;
    String signature = sha1Hex(paramsToSign + apiSecret);
    return new AvatarSignatureResponse(signature, timestamp, apiKey, cloudName, folder);
  }

  private String sha1Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-1 not available", e);
    }
  }
}
