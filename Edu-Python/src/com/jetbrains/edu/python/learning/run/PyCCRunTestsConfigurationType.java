package com.jetbrains.edu.python.learning.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.icons.AllIcons;
import com.jetbrains.edu.python.learning.messages.EduPythonBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class PyCCRunTestsConfigurationType implements ConfigurationType {
  @NotNull
  @Override
  public String getDisplayName() {
    return EduPythonBundle.message("tests.study.run");
  }

  @Override
  public String getConfigurationTypeDescription() {
    return EduPythonBundle.message("tests.study.runner");
  }

  @Override
  public Icon getIcon() {
    return AllIcons.Actions.Lightning;
  }

  @NotNull
  @Override
  public String getId() {
    return "ccruntests";
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{new PyCCRunTestsConfigurationFactory(this)};
  }

  public static PyCCRunTestsConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(PyCCRunTestsConfigurationType.class);
  }
}
