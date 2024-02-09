package com.jetbrains.edu.learning.feedback

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.showFeedbackSystemInfoDialog
import com.intellij.ui.dsl.builder.Panel

// BACKCOMPAT: 2023.2
typealias SystemDataJsonSerializable = com.intellij.platform.feedback.dialog.SystemDataJsonSerializable
typealias CommonFeedbackSystemData = com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
typealias BlockBasedFeedbackDialogWithEmail<T> = com.intellij.platform.feedback.dialog.BlockBasedFeedbackDialogWithEmail<T>
typealias FeedbackBlock = com.intellij.platform.feedback.dialog.uiBlocks.FeedbackBlock
typealias TopLabelBlock = com.intellij.platform.feedback.dialog.uiBlocks.TopLabelBlock
typealias DescriptionBlock = com.intellij.platform.feedback.dialog.uiBlocks.DescriptionBlock
typealias RatingBlock = com.intellij.platform.feedback.dialog.uiBlocks.RatingBlock
typealias TextAreaBlock = com.intellij.platform.feedback.dialog.uiBlocks.TextAreaBlock

fun showFeedbackSystemInfoDialog(project: Project?, systemInfoData: CommonFeedbackSystemData, addSpecificRows: Panel.() -> Unit) =
  showFeedbackSystemInfoDialog(project, systemInfoData, addSpecificRows)