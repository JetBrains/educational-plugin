package com.jetbrains.edu.learning.settings;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.ConfigurableEP;

public class OptionsProviderEP extends ConfigurableEP<OptionsProvider> {
  public static final ExtensionPointName<OptionsProviderEP>
    EP_NAME = ExtensionPointName.create("Educational.optionsProvider");
}
