package com.jetbrains.edu.learning.feedback

import com.intellij.feedback.common.dialog.showFeedbackSystemInfoDialog
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel

// BACKCOMPAT: 2023.2
typealias SystemDataJsonSerializable = com.intellij.feedback.common.dialog.JsonSerializable
typealias CommonFeedbackSystemData = com.intellij.feedback.common.dialog.CommonFeedbackSystemInfoData
typealias BlockBasedFeedbackDialogWithEmail<T> = com.intellij.feedback.common.dialog.BlockBasedFeedbackDialogWithEmail<T>
typealias FeedbackBlock = com.intellij.feedback.common.dialog.uiBlocks.FeedbackBlock
typealias TopLabelBlock = com.intellij.feedback.common.dialog.uiBlocks.TopLabelBlock
typealias DescriptionBlock = com.intellij.feedback.common.dialog.uiBlocks.DescriptionBlock
typealias RatingBlock = com.intellij.feedback.common.dialog.uiBlocks.RatingBlock
typealias TextAreaBlock = com.intellij.feedback.common.dialog.uiBlocks.TextAreaBlock

fun showFeedbackSystemInfoDialog(project: Project?, systemInfoData: CommonFeedbackSystemData, addSpecificRows: Panel.() -> Unit) =
  showFeedbackSystemInfoDialog(project, systemInfoData, addSpecificRows)