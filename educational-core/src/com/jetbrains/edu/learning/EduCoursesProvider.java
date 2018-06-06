package com.jetbrains.edu.learning;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.jetbrains.edu.learning.OpenApiExtKt.checkIsBackgroundThread;

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

  /**
   * Loads courses from all available providers
   *
   * @return list of loaded courses
   */
  static List<Course> loadAllCourses() {
    checkIsBackgroundThread();
    return Arrays.stream(Extensions.getExtensions(EP_NAME))
      .flatMap(provider -> provider.loadCourses().stream())
      .collect(Collectors.toList());
  }
}
