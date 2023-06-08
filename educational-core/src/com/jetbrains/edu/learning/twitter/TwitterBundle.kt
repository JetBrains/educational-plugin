package com.jetbrains.edu.learning.twitter;

import com.jetbrains.edu.learning.messages.EduPropertiesBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class TwitterBundle extends EduPropertiesBundle {
  @NonNls
  private static final String BUNDLE = "twitter.oauth_twitter";

  private TwitterBundle() {
    super(BUNDLE);
  }

  public static String value(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key) {
    return InstanceHolder.INSTANCE.valueOrEmpty(key);
  }

  private static class InstanceHolder {
    private static final TwitterBundle INSTANCE = new TwitterBundle();
  }
}
