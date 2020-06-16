package com.jetbrains.edu.coursecreator.actions.stepik.hyperskill

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduExperimentalFeatures.CC_HYPERSKILL
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import icons.EducationalCoreIcons

@Suppress("ComponentNotRegistered")
class ApplyHyperskillSubmission : DumbAwareAction(EduCoreBundle.message("hyperskill.educator.apply.submission"),
                                                  EduCoreBundle.message("hyperskill.educator.apply.submission"),
                                                  EducationalCoreIcons.JB_ACADEMY_ENABLED) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = getTask(project, e) ?: return

    val validator = object : InputValidatorEx {
      private var errorText: String? = null

      override fun checkInput(inputString: String?): Boolean {
        errorText = if (!StringUtil.isNotNegativeNumber(inputString))
          EduCoreBundle.message("hyperskill.educator.submission.invalid.id")
        else null

        return errorText == null
      }

      override fun getErrorText(inputString: String?): String? {
        return errorText
      }

      override fun canClose(inputString: String?): Boolean {
        return checkInput(inputString)
      }
    }

    val idText = Messages.showInputDialog(project, EduCoreBundle.message("hyperskill.educator.submission.id"),
                                          EduCoreBundle.message("hyperskill.educator.apply.submission"),
                                          null, null, validator) ?: return

    val id = Integer.valueOf(idText) // valid int because of validator

    computeUnderProgress(project, EduCoreBundle.message("hyperskill.educator.applying.submission"), false) {
      val submission = StepikConnector.getInstance().getSubmissionById(id).onError {
        Messages.showErrorDialog(EduCoreBundle.message("hyperskill.educator.failed.submission.message", id),
                                 EduCoreBundle.message("hyperskill.educator.failed.submission.title"))
        return@computeUnderProgress
      }
      // there is no information about step id in Stepik submissions, so we have to assume that it's a submission for current task
      submission.step = task.id
      HyperskillSolutionLoader.getInstance(project).updateTask(project, task, listOf(submission), true)
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    if (!isFeatureEnabled(CC_HYPERSKILL)) return

    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = getTask(project, e) != null
  }

  private fun getTask(project: Project, e: AnActionEvent): Task? {
    val course = StudyTaskManager.getInstance(project).course as? HyperskillCourse ?: return null
    if (course.isStudy) {
      return EduUtils.getCurrentTask(project)
    }

    val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
    if (selectedFiles == null || selectedFiles.size != 1) {
      return null
    }
    return EduUtils.getTask(project, course, selectedFiles[0])
  }
}