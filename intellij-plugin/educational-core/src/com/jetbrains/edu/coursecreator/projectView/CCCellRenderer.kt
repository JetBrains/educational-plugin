package com.jetbrains.edu.coursecreator.projectView

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.coursecreator.framework.SyncChangesTaskFileState
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import java.awt.*
import javax.swing.*
import javax.swing.tree.TreeCellRenderer

/**
 * Custom cell renderer for displaying sync changes state of task files in the project view
 */
class CCCellRenderer(private val innerRenderer: ColoredTreeCellRenderer) : JPanel(BorderLayout(JBUI.scale(2), 0)), TreeCellRenderer {
  private val iconHolder = JLabel()

  init {
    add(innerRenderer, BorderLayout.WEST)
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
    // render the default project view node component
    innerRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)

    val node = TreeUtil.getUserObject(value) as? CCFileNode
    if (!tryInstallNewIcon(node)) {
      // remove the icon if the node is not suitable
      iconHolder.icon = null
    }
    return this
  }

  private fun tryInstallNewIcon(node: CCFileNode?): Boolean {
    if (node == null || !isNodeInFrameworkLessonTask(node)) return false

    val presentation = node.presentation as? CCFilePresentationData ?: return false
    iconHolder.icon = getIcon(presentation.syncChangesState)
    return true
  }

  private fun getIcon(syncChangesTaskFileState: SyncChangesTaskFileState?): Icon? {
    return when (syncChangesTaskFileState) {
      SyncChangesTaskFileState.INFO -> AllIcons.General.Information
      SyncChangesTaskFileState.WARNING -> AllIcons.General.Warning
      null -> null
    }
  }

  companion object {
    private fun isNodeInFrameworkLessonTask(node: PsiFileNode): Boolean {
      // find the task using parent nodes, because finding the task using VFS in mouse adapter will be slower
      val taskNode = findAncestorTaskNode(node)
      val task = taskNode?.item
      return task?.lesson is FrameworkLesson
    }
  }
}
