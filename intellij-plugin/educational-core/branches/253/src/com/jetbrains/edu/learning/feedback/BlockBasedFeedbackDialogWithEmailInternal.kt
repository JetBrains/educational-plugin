package com.jetbrains.edu.learning.feedback

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.BlockBasedFeedbackDialogWithEmail
import com.intellij.platform.feedback.dialog.SystemDataJsonSerializable

// BACKCOMPAT: 2025.2. drop this proxy class and inline
abstract class BlockBasedFeedbackDialogWithEmailInternal<T: SystemDataJsonSerializable>(
  project: Project?,
  forTest: Boolean
): BlockBasedFeedbackDialogWithEmail<T>(project, forTest) {

  protected abstract fun computeSystemInfoDataInternal(): T

  override suspend fun computeSystemInfoData(): T = computeSystemInfoDataInternal()

  protected abstract fun showFeedbackDialogInternal(systemInfoData: T)

  override fun showFeedbackSystemInfoDialog(systemInfoData: T) = showFeedbackDialogInternal(systemInfoData)
}