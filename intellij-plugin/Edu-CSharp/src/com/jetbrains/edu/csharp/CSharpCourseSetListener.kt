package com.jetbrains.edu.csharp

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseSetListener
import com.jetbrains.edu.learning.courseFormat.Course

class CSharpCourseSetListener(private val project: Project) : CourseSetListener {
  override fun courseSet(course: Course) {
    if (course.languageId == "C#") {
      // make sure the service is loaded (needed only for C#, not for Unity)
      CSharpBackendService.getInstance(project)
    }
  }
}