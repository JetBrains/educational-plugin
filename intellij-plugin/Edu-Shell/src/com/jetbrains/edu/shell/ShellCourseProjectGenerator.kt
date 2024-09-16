package com.jetbrains.edu.shell

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.sh.shellcheck.ShShellcheckUtil
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings

class ShellCourseProjectGenerator(
  builder: EduCourseBuilder<EmptyProjectSettings>,
  course: Course
) : CourseProjectGenerator<EmptyProjectSettings>(builder, course) {
  override fun afterProjectGenerated(project: Project, projectSettings: EmptyProjectSettings, onConfigurationFinished: () -> Unit) {
    val onSuccess = Runnable {
      runReadAction {
        if (project.isDisposed) return@runReadAction
        EditorNotifications.getInstance(project).updateAllNotifications()
      }
    }
    val onFailure = Runnable { }
    ShShellcheckUtil.download(project, onSuccess, onFailure)
    super.afterProjectGenerated(project, projectSettings, onConfigurationFinished)
  }
}