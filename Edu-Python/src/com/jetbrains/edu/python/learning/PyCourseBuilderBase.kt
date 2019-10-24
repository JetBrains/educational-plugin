package com.jetbrains.edu.python.learning;

import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.python.newProject.PyNewProjectSettings;
import org.jetbrains.annotations.Nullable;

import static com.jetbrains.edu.python.learning.PyConfiguratorBase.TASK_PY;
import static com.jetbrains.edu.python.learning.PyConfiguratorBase.TESTS_PY;

public abstract class PyCourseBuilderBase implements EduCourseBuilder<PyNewProjectSettings> {

  @Nullable
  @Override
  public String getTaskTemplateName() {
    return TASK_PY;
  }

  @Nullable
  @Override
  public String getTestTemplateName() {
    return TESTS_PY;
  }
}
