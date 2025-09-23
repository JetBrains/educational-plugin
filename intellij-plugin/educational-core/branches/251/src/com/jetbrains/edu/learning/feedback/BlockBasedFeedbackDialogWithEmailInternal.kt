package com.jetbrains.edu.learning.feedback

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.BlockBasedFeedbackDialogWithEmail
import com.intellij.platform.feedback.dialog.SystemDataJsonSerializable

abstract class BlockBasedFeedbackDialogWithEmailInternal<T : SystemDataJsonSerializable>(
  project: Project?,
  forTest: Boolean
) : BlockBasedFeedbackDialogWithEmail<T>(project, forTest) {
  protected abstract fun computeSystemInfoDataInternal(): T

  override val mySystemInfoData: T by lazy {
    computeSystemInfoDataInternal()
  }

  protected abstract fun showFeedbackDialogInternal(systemInfoData: T)

  override val myShowFeedbackSystemInfoDialog: () -> Unit
    get() = { showFeedbackDialogInternal(mySystemInfoData) }
}