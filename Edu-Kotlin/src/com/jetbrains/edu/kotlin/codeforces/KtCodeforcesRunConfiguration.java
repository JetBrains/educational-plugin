package com.jetbrains.edu.kotlin.codeforces;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration;

import java.nio.file.Path;

import static com.intellij.openapi.module.ModuleUtilCore.findModuleForFile;
import static com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfigurationType.CONFIGURATION_ID;

public class KtCodeforcesRunConfiguration extends KotlinRunConfiguration implements CodeforcesRunConfiguration {
  public KtCodeforcesRunConfiguration(JavaRunConfigurationModule runConfigurationModule, ConfigurationFactory factory) {
    super(CONFIGURATION_ID, runConfigurationModule, factory);
  }

  @Nullable
  @Override
  public VirtualFile getRedirectInputFile() {
    String path = getInputRedirectOptions().getRedirectInputPath();
    if (path == null) return null;
    return VfsUtil.findFile(Path.of(path), true);
  }

  @Override
  public void setExecutableFile(@NotNull VirtualFile file) {
    setModule(findModuleForFile(file, getProject()));
    setRunClass("MainKt"); // don't like this, still search for possibility to get rid of it
  }
}
