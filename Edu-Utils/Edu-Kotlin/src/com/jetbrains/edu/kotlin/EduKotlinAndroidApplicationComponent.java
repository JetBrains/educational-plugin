package com.jetbrains.edu.kotlin;

import com.intellij.lang.Language;
import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

public class EduKotlinAndroidApplicationComponent implements ApplicationComponent {
  @Override
  public void initComponent() {
    new Language("kotlin-android") {

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
