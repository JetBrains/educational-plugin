package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse

object CodeforcesCourseUpdater {

  private val LOG: Logger = Logger.getInstance(CodeforcesCourseUpdater::class.java)

  fun updateCourse(project: Project, course: CodeforcesCourse, onFinish: () -> Unit) {
    println("UPDATE HAS BEEN CALLED")
    onFinish()
  }

}