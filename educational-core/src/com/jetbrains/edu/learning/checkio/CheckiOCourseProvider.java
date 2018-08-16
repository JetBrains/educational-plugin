package com.jetbrains.edu.learning.checkio;

import com.jetbrains.edu.learning.CoursesProvider;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public interface CheckiOCourseProvider extends CoursesProvider {
  @NotNull
  CheckiOCourse provideCourse();

  @NotNull
  @Override
  default List<Course> loadCourses() {
    return Collections.singletonList(provideCourse());
  }
}
