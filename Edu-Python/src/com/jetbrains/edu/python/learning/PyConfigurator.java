package com.jetbrains.edu.python.learning;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PlatformUtils;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.configuration.EduConfiguratorBase;
import com.jetbrains.edu.python.learning.checker.PyTaskCheckerProvider;
import com.jetbrains.python.newProject.PyNewProjectSettings;
import icons.PythonIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class PyConfigurator extends EduConfiguratorBase<PyNewProjectSettings> {
  public static final String TESTS_PY = "tests.py";
  public static final String TASK_PY = "task.py";

  private final PyCourseBuilder myCourseBuilder = new PyCourseBuilder();

  @NotNull
  @Override
  public EduCourseBuilder<PyNewProjectSettings> getCourseBuilder() {
    return myCourseBuilder;
  }

  @NotNull
  @Override
  public String getTestFileName() {
    return TESTS_PY;
  }

  @Override
  @NotNull
  public String getMockFileName(@NotNull String text) {
    return TASK_PY;
  }

  @Override
  public boolean excludeFromArchive(@NotNull Project project, @NotNull VirtualFile file) {
    String path = file.getPath();
    return super.excludeFromArchive(project, file) || path.contains("__pycache__") || path.endsWith(".pyc");
  }

  @Override
  public boolean isTestFile(@NotNull Project project, @NotNull VirtualFile file) {
    return TESTS_PY.equals(file.getName());
  }

  @Override
  public boolean isEnabled() {
    return !(PlatformUtils.isPyCharm() || PlatformUtils.isCLion());
  }

  @NotNull
  @Override
  public TaskCheckerProvider getTaskCheckerProvider() {
    return new PyTaskCheckerProvider();
  }

  @NotNull
  @Override
  public Icon getLogo() {
    return PythonIcons.Python.Python;
  }

  @Override
  public boolean isCourseCreatorEnabled() {
    return false;
  }
}
