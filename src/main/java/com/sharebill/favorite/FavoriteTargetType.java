package com.sharebill.favorite;

public final class FavoriteTargetType {
  public static final String USER = "user";
  public static final String CONTACT = "contact";

  private FavoriteTargetType() {
  }

  public static boolean isValid(String value) {
    return USER.equals(value) || CONTACT.equals(value);
  }
}
