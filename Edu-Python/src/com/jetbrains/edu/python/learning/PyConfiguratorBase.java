package com.jetbrains.edu.python.learning;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.configuration.EduConfiguratorWithSubmissions;
import com.jetbrains.edu.python.learning.checker.PyTaskCheckerProvider;
import com.jetbrains.python.newProject.PyNewProjectSettings;
import icons.PythonIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class PyConfiguratorBase extends EduConfiguratorWithSubmissions<PyNewProjectSettings> {
  public static final String TESTS_PY = "tests.py";
  public static final String TASK_PY = "task.py";

  private final PyCourseBuilderBase myCourseBuilder;

  public PyConfiguratorBase(@NotNull PyCourseBuilderBase courseBuilder) {
    myCourseBuilder = courseBuilder;
  }

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
    return super.excludeFromArchive(project, file) || PyEduUtils.excludeFromArchive(file);
  }

  @Override
  public boolean isTestFile(@NotNull Project project, @NotNull VirtualFile file) {
    return TESTS_PY.equals(file.getName());
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
