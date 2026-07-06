package com.sharebill.user;

public record AvatarSignatureResponse(
    String signature,
    long timestamp,
    String apiKey,
    String cloudName,
    String folder
) {
}
