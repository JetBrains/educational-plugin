package com.jetbrains.edu.learning.submissions

import com.intellij.ui.dsl.builder.SegmentedButton
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JButton

// BACKCOMPAT: 2023.2. Inline it
@Suppress("UnstableApiUsage")
fun SegmentedButton.ItemPresentation.segmentedButtonRenderer(item: JButton) {
  text = item.text
  enabled = item.isEnabled
  toolTipText = item.toolTipText
}

// BACKCOMPAT: 2023.2. Move to [com.jetbrains.edu.learning.submissions.SubmissionsTab.kt]
@Suppress("UnstableApiUsage")
fun SegmentedButton<JButton>.updateCommunityButton(isEnabled: Boolean, isAgreementTooltip: Boolean) {
  // todo: fix to `communityButton.text` after move
  val communityButton = items.findLast { it.text == EduCoreBundle.message("submissions.button.community") } ?: return
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
