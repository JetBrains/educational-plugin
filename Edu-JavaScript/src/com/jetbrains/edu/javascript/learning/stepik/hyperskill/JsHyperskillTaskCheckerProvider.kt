package com.jetbrains.edu.javascript.learning.stepik.hyperskill

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.javascript.learning.JsTaskChecker
import com.jetbrains.edu.javascript.learning.JsTaskCheckerProvider
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.stepik.hyperskill.SUCCESS_MESSAGE
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class JsHyperskillTaskCheckerProvider : JsTaskCheckerProvider() {
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    return object : JsTaskChecker(task, project) {
      override fun check(indicator: ProgressIndicator): CheckResult {
        val checkResult = super.check(indicator)
        val course = task.course
        if (checkResult.status == CheckStatus.Solved && course is HyperskillCourse) {
          return CheckResult(checkResult.status, SUCCESS_MESSAGE, needEscape = false)
        }
        return checkResult
      }
    }
  }
}
