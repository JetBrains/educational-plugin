package com.jetbrains.edu.python.learning.checkio;

import com.jetbrains.edu.learning.CoursesProvider;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.python.learning.checkio.courseFormat.PyCheckiOCourse;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PyCheckiOCourseProvider implements CoursesProvider {
  @NotNull
  @Override
  public List<Course> loadCourses() {
    return Collections.singletonList(new PyCheckiOCourse());
  }
}
