package com.jetbrains.edu.learning;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface EduCoursesProvider {

  ExtensionPointName<EduCoursesProvider> EP_NAME = ExtensionPointName.create("Educational.coursesProvider");

  /**
   * Loads courses from some source (plugin resources, Stepik, somewhere else).
   * Shouldn't be called from EDT thread.
   *
   * @return list of loaded courses
   */
  @NotNull
  List<Course> loadCourses();
}
