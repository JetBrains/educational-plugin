package com.jetbrains.edu.java.learning.stepik.alt;

import com.intellij.openapi.components.ApplicationComponent;

@SuppressWarnings("ComponentNotRegistered")
public class JHyperskillApplicationComponent implements ApplicationComponent {
  @Override
  public void initComponent() {
    new JHyperskill();
  }
}
