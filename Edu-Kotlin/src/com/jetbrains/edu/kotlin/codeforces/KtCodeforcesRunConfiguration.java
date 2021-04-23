package com.jetbrains.edu.kotlin.codeforces;

import com.intellij.execution.application.JvmMainMethodRunConfigurationOptions;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration;
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer;
import org.jetbrains.kotlin.psi.KtDeclarationContainer;

import static com.intellij.openapi.module.ModuleUtilCore.findModuleForFile;
import static com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfigurationType.CONFIGURATION_ID;

public class KtCodeforcesRunConfiguration extends KotlinRunConfiguration implements CodeforcesRunConfiguration {
  public KtCodeforcesRunConfiguration(JavaRunConfigurationModule runConfigurationModule, ConfigurationFactory factory) {
    super(CONFIGURATION_ID, runConfigurationModule, factory);
  }

  @Override
  public void setExecutableFile(@NotNull VirtualFile file) {
    setModule(findModuleForFile(file, getProject()));
    PsiElement element = PsiManager.getInstance(getProject()).findFile(file);
    if (element == null) {
      throw new IllegalStateException("Unable to find psiElement for " + file.getPath());
    }
    KtDeclarationContainer container = KotlinRunConfigurationProducer.Companion.getEntryPointContainer(element);
    if (container == null) {
      throw new IllegalStateException("Unable to set executable file for " + file.getPath());
    }
    String name = KotlinRunConfigurationProducer.Companion.getStartClassFqName(container);
    setRunClass(name);
  }

  @Override
  protected @NotNull Class<? extends ModuleBasedConfigurationOptions> getDefaultOptionsClass() {
    return JvmMainMethodRunConfigurationOptions.class;
  }
}
