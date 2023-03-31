package com.jetbrains.edu.python.learning.checkio.utils;

import com.jetbrains.edu.learning.messages.EduPropertiesBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class PyCheckiOOAuthBundle extends EduPropertiesBundle {
  @NonNls
  private static final String BUNDLE_NAME = "checkio.py-checkio-oauth";

  private PyCheckiOOAuthBundle() {
    super(BUNDLE_NAME);
  }

  public static String value(@NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key) {
    return InstanceHolder.INSTANCE.valueOrEmpty(key);
  }

  private static class InstanceHolder {
    private static final PyCheckiOOAuthBundle INSTANCE = new PyCheckiOOAuthBundle();
  }
}
