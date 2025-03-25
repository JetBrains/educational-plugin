package com.jetbrains.edu.learning.newproject.ui.filters

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeModelAdapter
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JTree
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
import javax.swing.event.TreeModelEvent
import javax.swing.tree.DefaultMutableTreeNode

abstract class FilterDropdown(
  var allItems: Set<String>,
  private val filterCourses: () -> Unit
) : JBLabel(AllIcons.General.LinkDropTriangle, SwingConstants.LEFT) {
  abstract val popupSize: Dimension
  abstract var selectedItems: Set<String>
  abstract val defaultTitle: String

  init {
    horizontalTextPosition = SwingConstants.LEFT
    iconTextGap = 2
    border = JBUI.Borders.empty(0, 15)

    this.addMouseListener(ShowPopupAdapter())
  }

  abstract fun isAccepted(course: Course): Boolean

  open fun updateItems(items: Set<String>) {
    allItems = items
  }

  abstract fun resetSelection()

  fun filter(courses: List<Course>): List<Course> {
    return courses.filter { isAccepted(it) }
  }

  private inner class ShowPopupAdapter : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      if (e.clickCount == 1) {
        val optionsTree = createTree()

        val pane = JBScrollPane(optionsTree).apply {
          horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
          preferredSize = popupSize
        }
        val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(pane, null).createPopup()
        val showPoint = Point(JBUIScale.scale(-10), height + JBUIScale.scale(4))
        popup.show(RelativePoint(this@FilterDropdown, showPoint))
      }
    }

    private fun createTree(): CheckboxTree {
      val root = CheckedTreeNode(allSelectedTitle())
      for (node in nodes()) {
        root.add(node)
      }

      return CheckboxTree(OptionNameCellRenderer(), root).apply {
        isRootVisible = true
        showsRootHandles = false
        model.addTreeModelListener(ApplyFiltersAdapter(this, root))
      }
    }

    private fun nodes(): List<CheckedTreeNode> = allItems.sorted().map {
      val node = CheckedTreeNode(it)
      node.isChecked = it in selectedItems
      node
    }
  }

  private inner class ApplyFiltersAdapter(private val optionsTree: CheckboxTree, private val root: CheckedTreeNode) : TreeModelAdapter() {
    override fun treeNodesChanged(e: TreeModelEvent) {
      val node = if (e.children.isNullOrEmpty()) e.path.last() else e.children.first()
      if (node !is CheckedTreeNode) {
        return
      }
      if (node.isRoot) {
        if (node.isChecked) {
          selectedItems = node.children().toList().map { (it as CheckedTreeNode).userObject as String }.toSet()
          text = allSelectedTitle()
        }
        else {
          selectedItems = emptySet()
          text = EduCoreBundle.message("course.dialog.filter.nothing.selected")
        }
      }
      else {
        val checkedItems = optionsTree.getCheckedNodes(String::class.java, null)
        text = when {
          checkedItems.isEmpty() -> EduCoreBundle.message("course.dialog.filter.nothing.selected")
          checkedItems.size == root.childCount -> allSelectedTitle()
          else -> checkedItems.joinToString(limit = 2)
        }
        selectedItems = checkedItems.toSet()
      }

      filterCourses()
    }
  }

  protected fun allSelectedTitle(): String = EduCoreBundle.message("course.dialog.filter.root.title", defaultTitle)
}

private class OptionNameCellRenderer : CheckboxTree.CheckboxTreeCellRenderer() {
  override fun customizeRenderer(tree: JTree,
                                 value: Any,
                                 selected: Boolean,
                                 expanded: Boolean,
                                 leaf: Boolean,
                                 row: Int,
                                 hasFocus: Boolean) {
    if (value is DefaultMutableTreeNode) {
      val userObject = value.userObject
      if (userObject is String) {
        textRenderer.append(userObject)
      }
    }
  }
}
