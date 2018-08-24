package com.jetbrains.edu.javascript.learning.checkio;

import com.intellij.openapi.components.ApplicationComponent;

@SuppressWarnings("ComponentNotRegistered") // registered in Java-Script.xml
public class JsCheckiOApplicationComponent implements ApplicationComponent {
  @Override
  public void initComponent() {
    new JsCheckiO();
  }
}
