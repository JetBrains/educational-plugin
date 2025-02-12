package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.AbstractPainter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeGlassPane
import com.intellij.openapi.wm.IdeGlassPaneUtil
import com.intellij.ui.GotItTooltip
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowViewImpl
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanelButtonComponent
import java.awt.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.SwingUtilities

class ShowImageAboveButtonAction : AnAction("ShowImageAboveButtonAction") {
    companion object {
        private fun showTooltip(component: IdeGlassPane, targetComponent: JComponent, image: BufferedImage, tooltip: GotItTooltip) {
            val glassPane = component as JComponent
            val point = SwingUtilities.convertPoint(glassPane, Point(0, 0), targetComponent)

            // Calculate position to center the image above the button
            val imageX = point.x + (targetComponent.width - 300) / 2
            val imageY = point.y - 300 - 5

            // Position tooltip at the top right corner of the image
            tooltip.show(targetComponent) { _, _ ->
                Point(imageX + image.width + 25, imageY + 10)
            }
        }
    }

    private var currentPainter: ImagePainter? = null

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val checkButton = findComponentInTaskToolWindow(project) ?: return

        // Create and add new painter
        val painter = ImagePainter(checkButton, project)
        val disposable = Disposer.newDisposable()
        Disposer.register(project, disposable)

        val glassPane = IdeGlassPaneUtil.find(checkButton as Component)
        glassPane.addPainter(checkButton, painter, disposable)
        currentPainter = painter

        val tooltip = GotItTooltip(
            "show.image.above.button.tooltip",
            "Das ist ZHABA",
            project
        )


        checkButton.repaint()

        val x = checkButton.location.x + painter.image.width + 100
        val y = checkButton.location.y + painter.image.height + 100
        tooltip.show(glassPane as JComponent) { _, _ ->
            Point(x, y)
        }
    }

    private fun findComponentInTaskToolWindow(project: Project): JComponent? {
        val toolWindowView = TaskToolWindowView.getInstance(project) as? TaskToolWindowViewImpl ?: return null
        val checkPanel = toolWindowView.checkPanel
        return checkPanel.checkButtonWrapper
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }


    private class ImagePainter(private val targetComponent: JComponent, project: Project) : AbstractPainter() {
        val image: BufferedImage = createImage()

        override fun executePaint(component: Component?, g: Graphics2D) {
            val glassPane = component as? JComponent ?: return
            val point = SwingUtilities.convertPoint(targetComponent, Point(0, 0), glassPane)

            // Enable anti-aliasing for smoother rendering
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

            // Calculate position to center the image above the button
            val x = point.x + (targetComponent.width - image.width) / 2
            val y = point.y - image.height - 5 // 5 pixels gap between button and image

            g.drawImage(image, x, y, null)
        }

        override fun needsRepaint(): Boolean = true

        private fun createImage(): BufferedImage {
            val imageUrl = javaClass.getResource("/img.png")
            val originalImage = ImageIO.read(imageUrl)

            val resizedImage = BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB)
            val g2d = resizedImage.createGraphics()
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g2d.drawImage(originalImage, 0, 0, 300, 300, null)
            } finally {
                g2d.dispose()
            }
            return resizedImage
        }
    }
}
