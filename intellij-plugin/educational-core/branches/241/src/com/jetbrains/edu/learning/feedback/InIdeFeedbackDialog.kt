@file:Suppress("UnstableApiUsage")

package com.jetbrains.edu.learning.feedback

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.BlockBasedFeedbackDialogWithEmail
import com.intellij.platform.feedback.dialog.showFeedbackSystemInfoDialog
import com.intellij.platform.feedback.dialog.uiBlocks.*
import com.intellij.platform.feedback.impl.notification.ThanksForFeedbackNotification
import com.intellij.ui.dsl.builder.Panel
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

abstract class InIdeFeedbackDialog<T : JbAcademyFeedbackSystemInfoData>(
  private val isStudent: Boolean,
  private val project: Project?,
  forTest: Boolean = false
) : BlockBasedFeedbackDialogWithEmail<T>(project, forTest) {

  override val myFeedbackJsonVersion: Int = super.myFeedbackJsonVersion + 1

  override val myBlocks: List<FeedbackBlock> = listOf(
    TopLabelBlock(EduCoreBundle.message("ui.feedback.dialog.title")),
    DescriptionBlock(EduCoreBundle.message("ui.feedback.dialog.description")),
    RatingBlock(EduCoreBundle.message("ui.feedback.dialog.rating.label"), "rate_impression"),
    TextAreaBlock(EduCoreBundle.message("ui.feedback.dialog.textarea.label"), "textarea_experience")
  )

  override val myFeedbackReportId: String = "academy_feedback"

  override val myShowFeedbackSystemInfoDialog: () -> Unit = {
    showJbAcademyFeedbackSystemInfoDialog(project, mySystemInfoData)
  }

  override val myTitle: String = EduCoreBundle.message("ui.feedback.dialog.top.title")
  override val zendeskFeedbackType: String = JBA_IN_IDE_FEEDBACK
  override val zendeskTicketTitle: String = JBA_IN_IDE_FEEDBACK

  public override fun showThanksNotification() {
    ThanksForFeedbackNotification(description = EduCoreBundle.message("ui.feedback.thanks.notification.content")).notify(project)
  }

  abstract fun showJbAcademyFeedbackSystemInfoDialog(project: Project?, systemInfoData: T)

  protected fun showSystemInfoDialog(project: Project?, systemInfoData: T, addSpecificRows: Panel.() -> Unit) {
    showFeedbackSystemInfoDialog(project, systemInfoData.commonSystemInfo) {
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode")) {
        if (isStudent) {
          label(EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode.student"))
        }
        else {
          label(EduCoreBundle.message("ui.feedback.dialog.system.info.course.mode.teacher"))
        }
      }
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name")) {
        label(systemInfoData.courseFeedbackInfoData.courseName)
      }
      row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.language")) {
        label(systemInfoData.courseFeedbackInfoData.courseLanguage)
      }
      addSpecificRows()
    }
  }

  companion object {
    @NonNls
    private const val JBA_IN_IDE_FEEDBACK = "JBAcademy in-IDE Feedback"
  }
}
