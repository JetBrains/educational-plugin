package com.jetbrains.edu.rust.codeforces;

import com.intellij.execution.InputRedirectAware;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import static com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfigurationType.CONFIGURATION_ID;

public class RsCodeforcesRunConfiguration extends CargoCommandConfiguration implements CodeforcesRunConfiguration {
  public RsCodeforcesRunConfiguration(Project project, ConfigurationFactory factory) {
    super(project, CONFIGURATION_ID, factory);
  }

  @Override
  public @NotNull
  InputRedirectAware.InputRedirectOptions getInputRedirectOptions() {
    return this;
  }
}
