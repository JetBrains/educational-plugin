package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.Disposable
import com.intellij.ui.JBCardLayout
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.newproject.ui.myCourses.MyCoursesProvider
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NotNull
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.ScrollPaneConstants
import javax.swing.tree.*


private const val FONT_SIZE = 13.0f
private const val PANEL_WIDTH = 1050
private const val PANEL_HEIGHT = 750

private const val SCROLL_PANE_WIDTH = 233

class CoursesPanelWithTabs(private val scope: CoroutineScope, disposable: @NotNull Disposable) : JPanel() {
  private val coursesTab: CoursesTab
  private val coursesProvidersTree: JBScrollPane
  private val myCoursesProvider: MyCoursesProvider = MyCoursesProvider(disposable)

  val languageSettings: LanguageSettings<*>? get() = coursesTab.languageSettings()
  val locationString get() = coursesTab.locationString()
  val selectedCourse get() = coursesTab.selectedCourse()

  init {
    layout = BorderLayout()
    coursesTab = CoursesTab()
    coursesProvidersTree = createCourseProvidersTree()
    add(coursesProvidersTree, BorderLayout.WEST)
    add(coursesTab, BorderLayout.CENTER)
    preferredSize = JBUI.size(PANEL_WIDTH, PANEL_HEIGHT)
  }

  private fun createCourseProvidersTree(): JBScrollPane {
    val root = DefaultMutableTreeNode("")
    val allCoursesNode = DefaultMutableTreeNode(message("course.dialog.all.courses"))
    CoursesPlatformProviderFactory.allProviders.forEach { allCoursesNode.add(DefaultMutableTreeNode(it)) }
    root.add(allCoursesNode)
    root.add(DefaultMutableTreeNode(myCoursesProvider))
    val tree = Tree(root).apply {
      isRootVisible = false
      rowHeight = 0 // force row to calculate size basing on its content
      showsRootHandles = false
      border = null
      cellRenderer = ProviderWithIconCellRenderer()
      TreeUtil.expandAll(this)
      focusListeners.forEach { removeFocusListener(it) }
      treeExpansionListeners.forEach { removeTreeExpansionListener(it) }
      setSelectionRow(1)
    }

    tree.addTreeSelectionListener {
      val node = it.path.lastPathComponent as DefaultMutableTreeNode
      val coursesProviderName = node.userObject as? CoursesPlatformProvider ?: return@addTreeSelectionListener
      coursesTab.showPanel(coursesProviderName.name)
    }

    return JBScrollPane(tree).apply {
      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
      preferredSize = JBUI.size(SCROLL_PANE_WIDTH, PANEL_HEIGHT)
      border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 1)
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

  fun loadCourses() {
    coursesTab.loadCourses(scope)
  }

  private inner class CoursesTab : JPanel() {
    private val panels: MutableList<CoursesPanel> = mutableListOf()
    private var activeTabName: String? = null
    private val cardLayout = JBCardLayout()

    init {
      layout = cardLayout
      val providers = CoursesPlatformProviderFactory.allProviders
      providers.forEach {
        addPanel(it)
      }

      addPanel(myCoursesProvider)
      showPanel(providers.first().name)
    }

    private fun addPanel(coursesPlatformProvider: CoursesPlatformProvider) {
      val panel = coursesPlatformProvider.createPanel(scope)
      panels.add(panel)
      add(coursesPlatformProvider.name, panel)
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
      val panel = cardLayout.findComponentById(activeTabName) as? CoursesPanel ?: return
      panel.onTabSelection()
      cardLayout.show(this, activeTabName)
    }

    fun doValidation() {
      (cardLayout.findComponentById(activeTabName) as CoursesPanel).doValidation()
    }

    fun selectedCourse(): Course? = currentPanel.selectedCourse

    fun locationString() = currentPanel.locationString

    fun languageSettings() = currentPanel.languageSettings

    fun setError(error: ErrorState) {
      currentPanel.setError(error)
    }

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
      val tabName = if (userObject is CoursesPlatformProvider) userObject.name else userObject.toString()
      when (userObject) {
        is MyCoursesProvider, is String -> {
          val additionalText = (userObject as? MyCoursesProvider)?.additionalText ?: ""
          textLabel.text = UIUtil.toHtml("<b>$tabName</b>$additionalText")
          iconLabel.icon = null
          component.border = JBUI.Borders.empty(PROVIDER_TOP_BOTTOM_OFFSET, 0)
        }
        is CoursesPlatformProvider -> {
          textLabel.text = tabName
          iconLabel.icon = userObject.icon
          component.border = JBUI.Borders.empty(PROVIDER_TOP_BOTTOM_OFFSET, PROVIDER_LEFT_OFFSET, PROVIDER_TOP_BOTTOM_OFFSET, 0)
        }
      }
      textLabel.foreground = UIUtil.getListForeground(selected, hasFocus)
    }

    return component
  }
}
