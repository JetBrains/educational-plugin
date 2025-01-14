package com.jetbrains.edu.aiHints.core.feedback.data

import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import kotlinx.serialization.Serializable

@Serializable
data class TextHintFeedbackSystemInfoData(
  override val commonSystemInfo: CommonFeedbackSystemData,
  override val hintFeedbackInfo: TextHintFeedbackInfoData
) : HintFeedbackSystemInfoData<TextHintFeedbackInfoData>()