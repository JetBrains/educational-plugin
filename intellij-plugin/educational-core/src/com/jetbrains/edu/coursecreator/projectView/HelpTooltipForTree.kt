package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.HelpTooltip
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JTree
import javax.swing.tree.TreeNode

class HelpTooltipForTree: SyncChangesHelpTooltip() {
  private val isTooltipEnabled = AtomicBoolean(false)
  private var currentHoveredNode: TreeNode? = null

  /**
   * @param callback Is called with the current mouse over the tree node. Use this node to return the list element data, and then
   *  configure the supplied [HelpTooltip]. Return `true` to enable the tooltip for the requested node, `false` when
   *  the node has no tooltip.
   */
  fun installOnTree(
    parentDisposable: Disposable,
    tree: JTree,
    callback: SyncChangesHelpTooltip.(node: TreeNode) -> Boolean
  ) : SyncChangesHelpTooltip {
    val mouseListener = object : MouseAdapter() {
      override fun mouseEntered(e: MouseEvent) = processMouse(e, tree, callback)
      override fun mouseExited(e: MouseEvent) = processMouse(e, tree, callback)
      override fun mouseMoved(e: MouseEvent) = processMouse(e, tree, callback)
    }

    tree.addMouseListener(mouseListener)
    tree.addMouseMotionListener(mouseListener)
    installOn(tree)
    setMasterPopupOpenCondition(tree) { isTooltipEnabled.get() }
    Disposer.register(parentDisposable) { dispose(tree) }
    return this
  }

  private fun processMouse(e: MouseEvent, tree: JTree, callback: SyncChangesHelpTooltip.(node: TreeNode) -> Boolean) {
    val row = tree.getRowForLocation(e.x, e.y)
    val node = if (row >= 0 && tree.getRowBounds(row).contains(e.point)) {
      tree.getPathForRow(row).lastPathComponent as? TreeNode
    } else {
      null
    }

    if (node == currentHoveredNode) {
      return
    }

    currentHoveredNode = node

    hidePopup(false)
    isTooltipEnabled.set(node != null && callback(node))
  }
}