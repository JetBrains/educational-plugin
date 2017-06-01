package com.jetbrains.edu.kotlin.android;

import com.intellij.lang.Language;
import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

public class EduKotlinAndroidApplicationComponent implements ApplicationComponent {
  public static String LANGUAGE = "kotlin-android";

  @Override
  public void initComponent() {
    new Language(LANGUAGE){};
  }

  @Override
  public void disposeComponent() {

  }

  @NotNull
  @Override
  public String getComponentName() {
    return "kotlin android";
  }
}
