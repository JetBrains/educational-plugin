package com.jetbrains.edu.javascript.learning.checkio;

import com.jetbrains.edu.javascript.learning.JsLanguageSettings;
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings;
import com.jetbrains.edu.javascript.learning.checkio.newProject.JsCheckiOCourseProjectGenerator;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.LanguageSettings;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsCheckiOCourseBuilder implements EduCourseBuilder<JsNewProjectSettings> {
  @Nullable
  @Override
  public String getTaskTemplateName() {
    return "";
  }

  @Nullable
  @Override
  public String getTestTemplateName() {
    return "";
  }

  @NotNull
  @Override
  public LanguageSettings<JsNewProjectSettings> getLanguageSettings() {
    return new JsLanguageSettings();
  }

  @Nullable
  @Override
  public CourseProjectGenerator<JsNewProjectSettings> getCourseProjectGenerator(@NotNull Course course) {
    return new JsCheckiOCourseProjectGenerator(this, course);
  }
}
