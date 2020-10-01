package com.jetbrains.edu.python.learning.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class PyCCRunTestsConfigurationFactory extends ConfigurationFactory {
  @NonNls
  public static final String CONFIGURATION_ID = "Run Study Tests";

  protected PyCCRunTestsConfigurationFactory(@NotNull ConfigurationType type) {
    super(type);
  }

  @NotNull
  @Override
  public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new PyCCRunTestConfiguration(project, this);
  }

  @Override
  public @NotNull String getId() {
    return CONFIGURATION_ID;
  }
}
