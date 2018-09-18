package com.jetbrains.edu.learning;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseLoading.BundledCoursesProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jetbrains.edu.learning.OpenApiExtKt.checkIsBackgroundThread;

public interface CoursesProvider {

  ExtensionPointName<CoursesProvider> EP_NAME = ExtensionPointName.create("Educational.coursesProvider");

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
  static List<Course> loadAllCourses(@NotNull List<CoursesProvider> providers) {
    checkIsBackgroundThread();

    List<Course> courses = new ArrayList<>();
    providers.sort((o1, o2) -> Boolean.compare(o1 instanceof BundledCoursesProvider, o2 instanceof BundledCoursesProvider));
    for (CoursesProvider provider : providers) {
      List<Course> providedCourses = provider.loadCourses();
      if (provider instanceof BundledCoursesProvider) {
        //do not add bundled course if there are the same remote courses
        for (Course providedCourse : providedCourses) {
          if (courses.stream().anyMatch(course -> course.getName().equals(providedCourse.getName()))) {
            continue;
          }
          courses.add(providedCourse);
        }
      }
      else {
       courses.addAll(providedCourses);
      }
    }
    return courses;
  }

  static List<Course> loadAllCourses() {
    return loadAllCourses(Arrays.asList(Extensions.getExtensions(EP_NAME)));
  }
}
