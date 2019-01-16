package com.jetbrains.edu.javascript.learning;

import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.LanguageSettings;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsCourseBuilder implements EduCourseBuilder<JsNewProjectSettings> {
  @Nullable
  @Override
  public String getTaskTemplateName() {
    return "task.js";
  }

  @Nullable
  @Override
  public String getTestTemplateName() {
    return "test.js";
  }

  @NotNull
  @Override
  public LanguageSettings<JsNewProjectSettings> getLanguageSettings() {
    return new JsLanguageSettings();
  }

  @Nullable
  @Override
  public CourseProjectGenerator<JsNewProjectSettings> getCourseProjectGenerator(@NotNull Course course) {
    return new JsCourseProjectGenerator(this, course);
  }
}
