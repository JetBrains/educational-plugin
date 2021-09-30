@file:JvmName("GotItTooltipExt")
package com.jetbrains.edu.learning

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.ProjectViewRenderer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.GotItTooltip
import com.jetbrains.edu.learning.projectView.CourseViewPane
import java.awt.Point
import javax.swing.JComponent

private val LOG = Logger.getInstance("com.jetbrains.edu.learning.GotItTooltipExt")


fun VirtualFile.showTooltipInCourseView(project: Project, tooltip: GotItTooltip) {
  if (!tooltip.canShow()) return
  ApplicationManager.getApplication().assertIsDispatchThread()

  val psiElement = document.toPsiFile(project) ?: return
  val projectView = ProjectView.getInstance(project)

  projectView.changeViewCB(CourseViewPane.ID, null).doWhenProcessed {
    projectView.selectCB(psiElement, this, true).doWhenProcessed {
      val tree = projectView.currentProjectViewPane.tree
      val location = JBPopupFactory.getInstance().guessBestPopupLocation(tree)

      val point = when (tooltip.position) {
        Balloon.Position.atRight -> {
          // icon width + text width
          val xOffset = ((tree.cellRenderer as ProjectViewRenderer).icon?.iconWidth ?: 0) +
                        (tree.getPathBounds(tree.selectionPath)?.width ?: 0)
          // 1/2 text height
          val yOffset = tree.getPathBounds(tree.selectionPath)?.height?.div(2) ?: 0
          location.point.addXOffset(xOffset).addYOffset(-yOffset)
        }
        else -> {
          LOG.warn("Unsupported position ${tooltip.position}")
          location.point
        }
      }

      val component = (location.component as JComponent)
      tooltip.show(component) { _, _ -> point }
    }
  }
}

private fun Point.addXOffset(offset: Int) = Point(x + offset, y)
private fun Point.addYOffset(offset: Int) = Point(x, y + offset)
