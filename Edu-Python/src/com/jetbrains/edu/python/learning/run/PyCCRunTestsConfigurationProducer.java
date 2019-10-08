package com.jetbrains.edu.python.learning.run;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.jetbrains.edu.python.learning.PyConfiguratorBase.TESTS_PY;

public class PyCCRunTestsConfigurationProducer extends LazyRunConfigurationProducer<PyCCRunTestConfiguration> {

  @Override
  protected boolean setupConfigurationFromContext(PyCCRunTestConfiguration configuration,
                                                  ConfigurationContext context,
                                                  Ref<PsiElement> sourceElement) {
    Project project = context.getProject();
    String testsPath = getTestPath(context);
    if (testsPath == null) {
      return false;
    }
    VirtualFile testsFile = LocalFileSystem.getInstance().findFileByPath(testsPath);
    if (testsFile == null) {
      return false;
    }

    String generatedName = generateName(testsFile, project);
    if (generatedName == null) {
      return false;
    }

    configuration.setPathToTest(testsPath);
    configuration.setName(generatedName);
    configuration.setScriptName(testsFile.getPath());
    return true;
  }

  @NotNull
  @Override
  public ConfigurationFactory getConfigurationFactory() {
    return PyCCRunTestsConfigurationType.getInstance().getConfigurationFactories()[0];
  }

  @Nullable
  private static String generateName(@NotNull VirtualFile testsFile, @NotNull Project project) {
    Task task = EduUtils.getTaskForFile(project, testsFile);
    if (task == null) {
      return null;
    }
    return task.getLesson().getName() + "/" + task.getName();
  }

  @Nullable
  private static String getTestPath(@NotNull ConfigurationContext context) {
    Location location = context.getLocation();
    if (location == null) {
      return null;
    }
    VirtualFile file = location.getVirtualFile();
    if (file == null) {
      return null;
    }
    Project project = location.getProject();
    Task task = EduUtils.getTaskForFile(project, file);
    if (task == null) {
      return null;
    }
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return null;
    }
    String testsFileName = TESTS_PY;
    String taskDirPath = FileUtil.toSystemDependentName(taskDir.getPath());
    String testsPath = taskDir.findChild(EduNames.SRC) != null ?
                       FileUtil.join(taskDirPath, EduNames.SRC, testsFileName) :
                       FileUtil.join(taskDirPath, testsFileName);
    String filePath = FileUtil.toSystemDependentName(file.getPath());
    return filePath.equals(testsPath) ? testsPath : null;
  }

  @Override
  public boolean isConfigurationFromContext(PyCCRunTestConfiguration configuration, ConfigurationContext context) {
    String path = getTestPath(context);
    return path != null && path.equals(configuration.getPathToTest());
  }
}
