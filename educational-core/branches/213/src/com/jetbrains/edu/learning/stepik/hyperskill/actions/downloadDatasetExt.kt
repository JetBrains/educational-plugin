package com.jetbrains.edu.learning.stepik.hyperskill.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.GotItTooltip
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.showTooltipInCourseView
import org.jetbrains.annotations.NonNls

@NonNls
const val TOOLTIP_ID: String = "downloaded.dataset.file"

fun showTooltipForDataset(project: Project, virtualFile: VirtualFile) {
  val message = EduCoreBundle.getMessage("hyperskill.dataset.here.is.your.dataset")
  val tooltip = GotItTooltip(TOOLTIP_ID, message, project)
    // Update width if message has been changed
    .withMaxWidth(JBUI.scale(270))
    .withPosition(Balloon.Position.atRight)

  if (tooltip.canShow()) {
    virtualFile.showTooltipInCourseView(project, tooltip)
  }
}