package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.HelpTooltip
import com.intellij.ide.projectView.impl.ProjectViewRenderer
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.openapi.project.Project
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.coursecreator.framework.getSyncChangesIcon
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.TreeCellRenderer

/**
 * Custom cell renderer for displaying sync changes state of task files in project view
 */
class CCCellRenderer(private val project: Project) : JPanel(BorderLayout()), TreeCellRenderer {
  private var iconHolder = JLabel()
  private val nodeRenderer = ProjectViewRenderer()

  init {
    add(nodeRenderer, BorderLayout.WEST)
    add(iconHolder, BorderLayout.CENTER)
  }

  override fun getTreeCellRendererComponent(
    tree: JTree?,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    invalidate()
    // render the default project view node component
    nodeRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)

    if (!tryInstallNewIcon(value)) {
      // remove old tooltip and remove icon if the node is not suitable
      HelpTooltip.dispose(iconHolder)
      iconHolder.icon = null
    }

    revalidate()
    return this
  }

  private fun tryInstallNewIcon(value: Any?): Boolean {
    val node = TreeUtil.getUserObject(value) as? PsiFileNode ?: return false
    if (!isNodeInFrameworkLessonTask(node)) return false

    val taskFile = node.virtualFile?.getTaskFile(project) ?: return false
    val newIcon = getSyncChangesIcon(taskFile) ?: return false

    if (iconHolder.icon == null) {
      HelpTooltip().apply {
        setTitle(EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ProjectView.Tooltip.text"))
        setDescription(EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ProjectView.Tooltip.description"))
        setLink(EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ActionLink.text")) {
          CCFrameworkLessonManager.getInstance(project).propagateChanges(taskFile.task, null)
        }
        installOn(iconHolder)
      }
    }
    iconHolder.icon = newIcon
    return true
  }

  companion object {
    private fun isNodeInFrameworkLessonTask(node: PsiFileNode): Boolean {
      // find the task using parent nodes, because finding the task using VFS in mouse adapter will be slower
      val taskNode = findAncestorTaskNode(node)
      val task = taskNode?.item
      return task?.lesson is FrameworkLesson
    }

    /**
     * Creates the mouse adapter that launches mouse listeners (of help tooltip) when the cursor is on the row with the icon
     */
    fun createMouseAdapter(tree: JTree, cellRenderer: CCCellRenderer): MouseAdapter = object : MouseAdapter() {
      private fun isOnCheckbox(e: MouseEvent?): Boolean {
        if (e == null) return false
        val row = tree.getRowForLocation(e.x, e.y)
        if (row < 0) return false
        val value = tree.getPathForRow(row).lastPathComponent

        val node = TreeUtil.getUserObject(value) as? PsiFileNode ?: return false
        if (!isNodeInFrameworkLessonTask(node)) return false

        val rowBounds = tree.getRowBounds(row)
        cellRenderer.bounds = rowBounds
        cellRenderer.validate()

        return cellRenderer.bounds.contains(e.point) && cellRenderer.iconHolder.isVisible
      }

      override fun mouseEntered(e: MouseEvent?) {
        if (!isOnCheckbox(e)) return
        for (mouseListener in cellRenderer.iconHolder.mouseListeners) {
          mouseListener.mouseEntered(e)
        }
      }

      override fun mouseExited(e: MouseEvent?) {
        if (!isOnCheckbox(e)) return
        for (mouseListener in cellRenderer.iconHolder.mouseListeners) {
          mouseListener.mouseExited(e)
        }
      }

      override fun mouseMoved(e: MouseEvent?) {
        if (!isOnCheckbox(e)) return
        for (mouseListener in cellRenderer.iconHolder.mouseMotionListeners) {
          mouseListener.mouseMoved(e)
        }
      }
    }
  }
}
