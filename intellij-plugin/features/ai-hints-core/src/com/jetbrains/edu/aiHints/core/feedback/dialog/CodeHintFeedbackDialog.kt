package com.jetbrains.edu.aiHints.core.feedback.dialog

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.showFeedbackSystemInfoDialog
import com.jetbrains.edu.ai.translation.ui.LikeBlock.FeedbackLikenessAnswer
import com.jetbrains.edu.aiHints.core.feedback.data.CodeHintFeedbackInfoData
import com.jetbrains.edu.aiHints.core.feedback.data.CodeHintFeedbackSystemInfoData
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.hints.hint.CodeHint
import com.jetbrains.educational.ml.hints.hint.TextHint

class CodeHintFeedbackDialog(
  private val project: Project,
  private val course: Course,
  private val task: Task,
  private val studentSolution: String,
  private val textHint: TextHint,
  private val codeHint: CodeHint,
  defaultLikeness: FeedbackLikenessAnswer = FeedbackLikenessAnswer.NO_ANSWER
) : HintFeedbackDialog<CodeHintFeedbackSystemInfoData>(project, defaultLikeness) {

  override fun showFeedbackSystemInfoDialog(systemInfoData: CodeHintFeedbackSystemInfoData) {
    showFeedbackSystemInfoDialog(project, systemInfoData.commonSystemInfo) {
      commonFeedbackData(systemInfoData.hintFeedbackInfo.hintFeedbackInfoData)
      row(EduAIHintsCoreBundle.message("hints.feedback.label.text.hint")) {
        label(systemInfoData.hintFeedbackInfo.textHint.text)
      }
      row(EduAIHintsCoreBundle.message("hints.feedback.label.code.hint")) {
        label(systemInfoData.hintFeedbackInfo.codeHint.code)
      }
    }
  }

  override suspend fun computeSystemInfoData(): CodeHintFeedbackSystemInfoData {
    return CodeHintFeedbackSystemInfoData(
      CommonFeedbackSystemData.getCurrentData(),
      CodeHintFeedbackInfoData.create(course, task, studentSolution, textHint, codeHint)
    )
  }

  init {
    init()
  }
}