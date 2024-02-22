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
fun SegmentedButton<JButton>.enableCommunityButton() {
  // todo: fix to `communityButton.text` after move
  val communityButton = items.findLast { it.text == EduCoreBundle.message("submissions.button.community") } ?: return
  communityButton.isEnabled = true
  communityButton.toolTipText = EduCoreBundle.message("submissions.button.community.tooltip.text.enabled")
  update(communityButton)
}

// BACKCOMPAT: 2023.2. Move to [com.jetbrains.edu.learning.submissions.SubmissionsTab.kt]
@Suppress("UnstableApiUsage")
fun SegmentedButton<JButton>.disableCommunityButton(isAgreementTooltip: Boolean = false) {
  // todo: fix to `communityButton.text` after move
  val communityButton = items.findLast { it.text == EduCoreBundle.message("submissions.button.community") } ?: return
  communityButton.isEnabled = false
  communityButton.toolTipText = if (isAgreementTooltip) {
    EduCoreBundle.message("submissions.tab.solution.sharing.agreement")
  }
  else {
    EduCoreBundle.message("submissions.button.community.tooltip.text.disabled")
  }
  selectedItem = items.first()
  update(communityButton)
}
