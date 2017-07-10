package com.jetbrains.edu.kotlin.android;

import com.intellij.lang.Language;
import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

public class EduKotlinAndroidApplicationComponent implements ApplicationComponent {
  @Override
  public void initComponent() {
    new Language("edu-kotlin-android") {
      @NotNull
      @Override
      public String getDisplayName() {
        return "Android";
      }
    };
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
