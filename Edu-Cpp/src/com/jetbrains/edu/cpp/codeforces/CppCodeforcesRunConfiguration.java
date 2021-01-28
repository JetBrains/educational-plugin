package com.jetbrains.edu.cpp.codeforces;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfigurationType;
import com.jetbrains.cidr.cpp.execution.CMakeRunConfigurationType;
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData;
import com.jetbrains.cidr.execution.BuildTargetData;
import com.jetbrains.cidr.execution.ExecutableData;
import com.jetbrains.edu.learning.VirtualFileExt;
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfigurationType.CONFIGURATION_ID;

public class CppCodeforcesRunConfiguration extends CMakeAppRunConfiguration implements CodeforcesRunConfiguration {
  public CppCodeforcesRunConfiguration(Project project, ConfigurationFactory factory) {
    super(project, factory, CONFIGURATION_ID);
  }

  @Override
  public void setExecutableFile(@NotNull VirtualFile file) {
    BuildTargetData buildTargetData = new BuildTargetData(getProject().getName(), getTargetName(file));
    setExecutableData(new ExecutableData(buildTargetData));
    setTargetAndConfigurationData(new BuildTargetAndConfigurationData(buildTargetData, null));
  }

  private @NotNull String getTargetName(@NotNull VirtualFile file) {
    Project project = getProject();
    TaskFile taskFile = VirtualFileExt.getTaskFile(file, project);
    if (taskFile == null) {
      throw new IllegalStateException("Unable to find taskFile for virtual file " + file.getPath());
    }
    return taskFile.getTask().getName() + "-run";
  }

  @Override
  public @NotNull InputRedirectOptions getInputRedirectOptions() {
    return this;
  }

  @Override
  public @NotNull CMakeRunConfigurationType getType() {
    return new CMakeAppRunConfigurationType();
  }
}
