package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseUpdateChecker
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.Course

class CodeforcesCourseUpdateChecker(project: Project,
                                    course: CodeforcesCourse,
                                    disposable: Disposable
) : CourseUpdateChecker<CodeforcesCourse>(project, course, disposable) {

  init {
    setCustomCheckInterval(60)
  }

  override fun Course.canBeUpdated(): Boolean = course is CodeforcesCourse

  override fun doCheckIsUpToDate(onFinish: () -> Unit) {
    CodeforcesCourseUpdater(project, course).updateCourse { onFinish() }
  }
}
