package com.jetbrains.edu.python.learning.checkio;

import com.intellij.openapi.components.ApplicationComponent;

public class PyCheckiOApplicationComponent implements ApplicationComponent {
  @Override
  public void initComponent() {
    new PyCheckiO();
  }
}
