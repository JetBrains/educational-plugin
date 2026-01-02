package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.ZHABA_DIMENSION
import java.awt.Point

/**
 * Ensures the project tool window is open and returns the relative point in which Zhaba is
 * located in the middle bottom.
 */
internal fun locateZhabaInProjectToolWindow(project: Project): RelativePoint? {
  val projectViewToolWindow = ToolWindowManager.getInstance(project)
                                .getToolWindow("Project") ?: return null
  projectViewToolWindow.show()

  val component = projectViewToolWindow.component
  if (!component.isShowing) return null

  // Position of the zhaba on the project view component
  val zhabaPoint = Point(
    (component.width - ZHABA_DIMENSION.width) / 2,
    component.height - ZHABA_DIMENSION.height
  )
  return RelativePoint(component, zhabaPoint)
}