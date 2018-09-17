package com.jetbrains.edu.learning.courseLoading

import com.google.common.collect.Lists
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.EduUtils.execCancelable
import com.jetbrains.edu.learning.courseFormat.Course
import java.util.concurrent.Callable


object CourseLoader {
  /**
   * @return null if process was canceled, otherwise not null list of courses
   */
  @JvmStatic
  fun getCourseInfosUnderProgress(): List<Course>? {
    try {
      return ProgressManager.getInstance().runProcessWithProgressSynchronously<List<Course>, RuntimeException>(
        {
          ProgressManager.getInstance().progressIndicator.isIndeterminate = true
          val courses = execCancelable<List<Course>>(Callable<List<Course>> { CoursesProvider.loadAllCourses() })
          if (courses == null) return@runProcessWithProgressSynchronously emptyList()
          courses.sortedBy(Course::isAdaptive)
          courses
        }, "Getting Available Courses", true, null)
    }
    catch (e: ProcessCanceledException) {
      return null
    }
    catch (e: RuntimeException) {
      return Lists.newArrayList()
    }
  }
}