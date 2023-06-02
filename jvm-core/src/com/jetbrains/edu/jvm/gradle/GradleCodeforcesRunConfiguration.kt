package com.jetbrains.edu.jvm.gradle;

import com.intellij.execution.Executor;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.jetbrains.edu.jvm.MainFileProvider;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfigurationType.CONFIGURATION_ID;

public class GradleCodeforcesRunConfiguration extends ApplicationConfiguration implements CodeforcesRunConfiguration {
  private static final Logger LOG = Logger.getInstance(GradleCodeforcesRunConfiguration.class);

  public GradleCodeforcesRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory) {
    super(CONFIGURATION_ID, project, factory);
  }

  @Override
  public void setExecutableFile(@NotNull VirtualFile file) {
    Course course = OpenApiExtKt.getCourse(getProject());
    if (course == null) {
      LOG.error("Unable to find course");
      return;
    }
    Language language = CourseExt.getLanguageById(course);
    if (language == null) {
      LOG.error("Unable to get language for course " + course.getPresentableName());
      return;
    }

    PsiClass mainClass = ((PsiClass)MainFileProvider.getMainClass(getProject(), file, language));
    if (mainClass == null) {
      LOG.error("Unable to find main class for file " + file.getPath());
      return;
    }

    setMainClass(mainClass);
  }

  // TODO: remove this after input file substitution doesn't depend on id in the platform
  // This method overriding is needed because currently input for java is redirected for specific configurations only
  // see com.intellij.execution.InputRedirectAware.TYPES_WITH_REDIRECT_AWARE_UI
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) {
    JavaApplicationCommandLineState<GradleCodeforcesRunConfiguration> state =
      GradleCodeforcesUtils.getJavaApplicationCommandLineState(this, env);

    JavaRunConfigurationModule module = getConfigurationModule();
    state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject(), module.getSearchScope()));

    return state;
  }
}
