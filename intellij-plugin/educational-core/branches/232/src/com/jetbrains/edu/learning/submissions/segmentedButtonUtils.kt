package com.jetbrains.edu.learning.submissions

import com.intellij.ui.dsl.builder.SegmentedButton

fun segmentedButtonRenderer(item: SubmissionsTab.SegmentedButtonItem): String = item.text

@Suppress("UnstableApiUsage", "UnusedReceiverParameter", "UNUSED_PARAMETER")
fun SegmentedButton<SubmissionsTab.SegmentedButtonItem>.updateCommunityButton(isEnabled: Boolean, isAgreementTooltip: Boolean) {}
