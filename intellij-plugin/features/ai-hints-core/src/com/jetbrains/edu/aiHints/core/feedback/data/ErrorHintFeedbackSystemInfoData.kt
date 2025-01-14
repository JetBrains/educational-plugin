package com.jetbrains.edu.aiHints.core.feedback.data

import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import kotlinx.serialization.Serializable

@Serializable
data class ErrorHintFeedbackSystemInfoData(
  override val commonSystemInfo: CommonFeedbackSystemData,
  override val hintFeedbackInfo: ErrorHintFeedbackInfoData
) : HintFeedbackSystemInfoData<ErrorHintFeedbackInfoData>()