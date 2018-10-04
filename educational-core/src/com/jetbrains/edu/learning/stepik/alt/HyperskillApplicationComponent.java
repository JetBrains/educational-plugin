package com.jetbrains.edu.learning.stepik.alt;

import com.intellij.openapi.components.ApplicationComponent;

@SuppressWarnings("ComponentNotRegistered")
public class HyperskillApplicationComponent implements ApplicationComponent {
  @Override
  public void initComponent() {
    new Hyperskill();
  }
}
