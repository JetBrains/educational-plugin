package com.jetbrains.edu.javascript.learning.checkio;

import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames;
import com.jetbrains.edu.learning.CoursesProvider;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class JsCheckiOCourseProvider implements CoursesProvider {
  @NotNull
  @Override
  public List<Course> loadCourses() {
    final Course course = new CheckiOCourse(JsCheckiONames.JS_CHECKIO, JsCheckiONames.JS_CHECKIO_LANGUAGE);
    if (EduConfiguratorManager.forLanguage(course.getLanguageById()) != null) {
      return Collections.singletonList(course);
    }
    return Collections.emptyList();
  }
}
