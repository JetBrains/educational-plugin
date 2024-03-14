package com.jetbrains.edu.learning.submissions

import com.intellij.ui.dsl.builder.SegmentedButton
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JButton

// BACKCOMPAT: 2023.2. Inline it.
@Suppress("UnstableApiUsage")
fun SegmentedButton.ItemPresentation.segmentedButtonRenderer(item: JButton) {
  text = item.text
  enabled = item.isEnabled
  toolTipText = item.toolTipText
}

// BACKCOMPAT: 2023.2
@Suppress("UnstableApiUsage")
fun SegmentedButton<JButton>.updateCommunityButton(isEnabled: Boolean, isAgreementTooltip: Boolean = false) {
  val communityButton = items.findLast { it.text == SubmissionsTab.COMMUNITY.text } ?: return

  communityButton.isEnabled = isEnabled
  communityButton.toolTipText = when {
    isAgreementTooltip -> EduCoreBundle.message("submissions.tab.solution.sharing.agreement")
    isEnabled -> EduCoreBundle.message("submissions.button.community.tooltip.text.enabled")
    else -> EduCoreBundle.message("submissions.button.community.tooltip.text.disabled")
  }

  if (!isEnabled) {
    selectedItem = items.first()
  }

  update(communityButton)
}
