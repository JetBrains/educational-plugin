package com.jetbrains.edu.jvm.stepik.hyperskill

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.jvm.gradle.checker.NewGradleEduTaskChecker
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillGradleTaskCheckerProvider : GradleTaskCheckerProvider() {
  override fun mainClassForFile(project: Project, file: VirtualFile): String? = null

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    return object : NewGradleEduTaskChecker(task, project) {
      override fun check(indicator: ProgressIndicator): CheckResult {
        val checkResult = super.check(indicator)
        val course = task.course
        if (checkResult.status == CheckStatus.Solved && course is HyperskillCourse) {
          return CheckResult(checkResult.status, SUCCESS, needEscape = false)
        }
        return checkResult
      }
    }
  }

  companion object {
    private const val SUCCESS = "<html>${TestsOutputParser.CONGRATULATIONS} " +
                                "Continue on <a href=\"https://hyperskill.org/learning-path\">Hyperskill</a>.</html>"
  }
}
