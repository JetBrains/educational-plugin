package com.jetbrains.edu.learning.settings;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableEP;

public interface OptionsProvider extends Configurable {
  ExtensionPointName<ConfigurableEP<OptionsProvider>> EP_NAME = ExtensionPointName.create("Educational.optionsProvider");
}
