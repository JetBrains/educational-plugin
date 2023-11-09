package com.jetbrains.edu.learning.submissions

import com.intellij.ui.dsl.builder.SegmentedButton
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JButton

@Suppress("UnstableApiUsage")
fun SegmentedButton.ItemPresentation.segmentedButtonRenderer(item: JButton) {
  text = item.text
  enabled = item.isEnabled
  toolTipText = item.toolTipText
}

@Suppress("UnstableApiUsage")
fun SegmentedButton<JButton>.enableCommunityButton() {
  val communityButton = items.last()
  communityButton.isEnabled = true
  communityButton.toolTipText = EduCoreBundle.message("submissions.button.community.tooltip.text.enabled")
  update(communityButton)
}

@Suppress("UnstableApiUsage")
fun SegmentedButton<JButton>.disableCommunityButton() {
  val communityButton = items.last()
  communityButton.isEnabled = false
  communityButton.toolTipText = EduCoreBundle.message("submissions.button.community.tooltip.text.disabled")
  selectedItem = items.first()
  update(communityButton)
}