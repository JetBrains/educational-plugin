package com.jetbrains.edu.learning.feedback

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.BlockBasedFeedbackDialogWithEmail
import com.intellij.platform.feedback.dialog.SystemDataJsonSerializable

// BACKCOMPAT: 2025.3. drop this proxy class and inline
abstract class BlockBasedFeedbackDialogWithEmailInternal<T: SystemDataJsonSerializable>(project: Project?, forTest: Boolean): BlockBasedFeedbackDialogWithEmail<T>(project, forTest) {

  abstract fun computeSystemInfoDataInternal(): T

  override suspend fun computeSystemInfoData(): T = computeSystemInfoDataInternal()

  abstract fun showFeedbackDialogInternal()

  override fun showFeedbackSystemInfoDialog(systemInfoData: T) = showFeedbackDialogInternal()
}