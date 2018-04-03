package com.jetbrains.edu.python.learning;

import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator;
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings;
import com.jetbrains.python.newProject.PyNewProjectSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.jetbrains.edu.python.learning.PyConfigurator.TASK_PY;
import static com.jetbrains.edu.python.learning.PyConfigurator.TESTS_PY;

public class PyCourseBuilder implements EduCourseBuilder<PyNewProjectSettings> {

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

  @NotNull
  @Override
  public LanguageSettings<PyNewProjectSettings> getLanguageSettings() {
    return new PyLanguageSettings();
  }

  @Nullable
  @Override
  public CourseProjectGenerator<PyNewProjectSettings> getCourseProjectGenerator(@NotNull Course course) {
    return new PyCourseProjectGenerator(this, course);
  }
}
