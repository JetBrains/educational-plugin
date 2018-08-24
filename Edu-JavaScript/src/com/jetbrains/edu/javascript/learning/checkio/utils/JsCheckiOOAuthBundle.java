package com.jetbrains.edu.javascript.learning.checkio.utils;

import com.intellij.CommonBundle;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.util.ResourceBundle;

public class JsCheckiOOAuthBundle {
  private JsCheckiOOAuthBundle() {}

  public static String messageOrDefault(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull String defaultValue, @NotNull Object... params) {
    return CommonBundle.messageOrDefault(getBundle(), key, defaultValue, params);
  }

  private static Reference<ResourceBundle> ourBundle;

  @NonNls
  private static final String BUNDLE = "checkio.js-checkio-oauth";

  @NotNull
  private static ResourceBundle getBundle() {
    ResourceBundle bundle = SoftReference.dereference(ourBundle);
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE);
      ourBundle = new SoftReference<>(bundle);
    }
    return bundle;
  }
}
