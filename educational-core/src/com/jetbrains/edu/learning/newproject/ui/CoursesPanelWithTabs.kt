package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ui.JBCardLayout
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.newproject.ui.coursePanel.NewCoursePanel
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.ScrollPaneConstants
import javax.swing.tree.*


private const val FONT_SIZE = 13.0f
private const val SCROLL_PANE_WIDTH = 233
private const val SCROLL_PANE_HEIGHT = 800

class CoursesPanelWithTabs : JPanel() {
  private val coursesTab: CoursesTab
  private val coursesProvidersTree: JBScrollPane

  val projectSettings get() = coursesTab.projectSettings()
  val locationString get() = coursesTab.locationString()
  val selectedCourse get() = coursesTab.selectedCourse()

  init {
    layout = BorderLayout()
    coursesTab = CoursesTab()
    coursesProvidersTree = createCourseProvidersTree()
    add(coursesProvidersTree, BorderLayout.WEST)
    add(coursesTab, BorderLayout.CENTER)
  }

  private fun createCourseProvidersTree(): JBScrollPane {
    val root = DefaultMutableTreeNode(message("course.dialog.all.courses"))
    CoursesPlatformProviderFactory.allProviders.forEach { root.add(DefaultMutableTreeNode(it)) }
    val tree = Tree(root)
    tree.apply {
      rowHeight = 0 // force row to calculate size basing on its content
      showsRootHandles = false
      border = null
      cellRenderer = ProviderWithIconCellRenderer()
      selectionModel = UnselectableRootSelectionModel()
      setSelectionRow(1)
      focusListeners.forEach { removeFocusListener(it) }
      treeExpansionListeners.forEach { removeTreeExpansionListener(it) }
    }

    tree.addTreeSelectionListener {
      val node = it.path.lastPathComponent as DefaultMutableTreeNode
      val coursesProviderName = node.userObject as? CoursesPlatformProvider ?: return@addTreeSelectionListener
      coursesTab.showPanel(coursesProviderName.name)
    }

    return JBScrollPane(tree).apply {
      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
      preferredSize = JBUI.size(SCROLL_PANE_WIDTH, SCROLL_PANE_HEIGHT)
      border = JBUI.Borders.customLine(NewCoursePanel.DIVIDER_COLOR, 0, 0, 0, 1)
    }
  }

  fun setError(error: ErrorState) {
    coursesTab.setError(error)
  }

  fun doValidation() {
    coursesTab.doValidation()
  }

  fun setSidePaneBackground() {
    UIUtil.setBackgroundRecursively(coursesProvidersTree, UIUtil.SIDE_PANEL_BACKGROUND)
  }

  fun loadCourses(scope: CoroutineScope) {
    coursesTab.loadCourses(scope)
  }

  private inner class CoursesTab : JPanel() {
    // TODO: can't we get it from layout? See other usages
    private val panels: MutableList<CoursesPanel> = mutableListOf()
    private var activeTabName: String? = null
    private val cardLayout = JBCardLayout()

    init {
      layout = cardLayout
      val providers = CoursesPlatformProviderFactory.allProviders
      providers.forEach {
        val panel = it.panel
        panels.add(panel)
        add(it.name, panel)
      }
      showPanel(providers.first().name)
    }

    fun loadCourses(scope: CoroutineScope) {
      panels.forEach {
        scope.launch {
          it.loadCourses()
        }
      }
    }

    fun showPanel(name: String) {
      activeTabName = name
      cardLayout.show(this, activeTabName)
    }

    fun doValidation() {
      (cardLayout.findComponentById(activeTabName) as CoursesPanel).doValidation()
    }

    fun selectedCourse(): Course? = currentPanel.selectedCourse

    fun locationString() = currentPanel.locationString

    fun projectSettings() = currentPanel.projectSettings

    fun setError(error: ErrorState) {
      currentPanel.setError(error)
    }

    // TODO: get from panels?
    private val currentPanel: CoursesPanel
      get() {
        activeTabName ?: error("Active tab name is null")
        val activeComponent = (layout as JBCardLayout).findComponentById(activeTabName)
        return activeComponent as CoursesPanel
      }
  }
}

private class UnselectableRootSelectionModel : DefaultTreeSelectionModel() {

  init {
    selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    resetRowSelection()
  }

  private fun canSelectPath(treePath: TreePath): Boolean = treePath.pathCount > 1

  private fun getFilteredPaths(paths: Array<TreePath>?): Array<TreePath>? = paths?.filter { canSelectPath(it) }?.toTypedArray()

  override fun setSelectionPath(path: TreePath?) {
    if (canSelectPath(path!!)) {
      super.setSelectionPath(path)
    }
  }

  override fun setSelectionPaths(paths: Array<TreePath>?) = super.setSelectionPaths(getFilteredPaths(paths))

}

private const val PROVIDER_TOP_BOTTOM_OFFSET = 11
private const val PROVIDER_LEFT_OFFSET = 5
private const val ICON_TEXT_GAP = 8

private class ProviderWithIconCellRenderer : DefaultTreeCellRenderer() {
  private val component = JPanel(FlowLayout(FlowLayout.LEFT, ICON_TEXT_GAP, 0))
  private val textLabel = JBLabel()
  private val iconLabel = JBLabel()

  init {
    textLabel.font = Font(TypographyManager().bodyFont, Font.PLAIN, JBUI.scaleFontSize(FONT_SIZE))
    component.border = JBUI.Borders.empty(PROVIDER_TOP_BOTTOM_OFFSET, 0)
    component.add(iconLabel)
    component.add(textLabel)
  }

  override fun getTreeCellRendererComponent(tree: JTree?,
                                            value: Any?,
                                            selected: Boolean,
                                            expanded: Boolean,
                                            leaf: Boolean,
                                            row: Int,
                                            hasFocus: Boolean): Component {
    if (value is DefaultMutableTreeNode) {
      val userObject = value.userObject
      if (userObject is CoursesPlatformProvider) {
        iconLabel.icon = userObject.icon
        textLabel.text = userObject.name
        component.border = JBUI.Borders.empty(PROVIDER_TOP_BOTTOM_OFFSET, PROVIDER_LEFT_OFFSET, PROVIDER_TOP_BOTTOM_OFFSET, 0)
      }

      if (userObject is String) {
        isEnabled = false
        iconLabel.icon = null
        textLabel.text = UIUtil.toHtml("<b>$userObject</b>")
        component.border = JBUI.Borders.empty(PROVIDER_TOP_BOTTOM_OFFSET, 0)
      }
    }

    return component
  }
}
