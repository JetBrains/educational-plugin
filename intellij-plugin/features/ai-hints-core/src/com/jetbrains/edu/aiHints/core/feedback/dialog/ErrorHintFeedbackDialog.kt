package com.jetbrains.edu.aiHints.core.feedback.dialog

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.showFeedbackSystemInfoDialog
import com.intellij.platform.feedback.dialog.uiBlocks.FeedbackBlock
import com.intellij.platform.feedback.dialog.uiBlocks.TextAreaBlock
import com.jetbrains.edu.aiHints.core.feedback.data.ErrorHintFeedbackInfoData
import com.jetbrains.edu.aiHints.core.feedback.data.ErrorHintFeedbackSystemInfoData
import com.jetbrains.edu.aiHints.core.log.Logger
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle

class ErrorHintFeedbackDialog(
  private val project: Project,
  private val course: Course,
  private val task: Task,
  private val studentSolution: String,
  private val errorMessage: String
) : HintFeedbackDialog<ErrorHintFeedbackSystemInfoData>(project) {
  override val myBlocks: List<FeedbackBlock> = listOf(
    TextAreaBlock("", "hints_experience")
      .setPlaceholder(EduCoreBundle.message("ui.feedback.dialog.textarea.optional.label"))
  )

  override val myShowFeedbackSystemInfoDialog: () -> Unit = {
    showFeedbackSystemInfoDialog(project, mySystemInfoData.commonSystemInfo) {
      commonFeedbackData(mySystemInfoData.hintFeedbackInfo.hintFeedbackInfoData)
      row(EduAIHintsCoreBundle.message("hints.feedback.label.error.message")) {
        label(mySystemInfoData.hintFeedbackInfo.errorMessage)
      }
    }
  }

  override val mySystemInfoData: ErrorHintFeedbackSystemInfoData by lazy {
    ErrorHintFeedbackSystemInfoData(
      CommonFeedbackSystemData.getCurrentData(),
      ErrorHintFeedbackInfoData.create(course, task, studentSolution, errorMessage)
    )
  }

  init {
    init()
  }

  override fun sendFeedbackData() {
    super.sendFeedbackData()
    Logger.aiHintsLogger.info(
      """|| Course id: ${task.course.id} | Lesson id: ${task.lesson.id} | Task id: ${task.id}
         || Feedback: ${collectDataToJsonObject()}
         || Type: error hint
      """.trimMargin()
    )
  }
}