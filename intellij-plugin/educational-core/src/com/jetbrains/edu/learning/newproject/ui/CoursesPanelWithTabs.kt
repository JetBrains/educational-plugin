package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.JBCardLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.newproject.coursesStorage.CourseDeletedListener
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorageBase
import com.jetbrains.edu.learning.newproject.ui.myCourses.MyCoursesProvider
import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode

private const val PANEL_WIDTH = 1050
private const val PANEL_HEIGHT = 750

class CoursesPanelWithTabs(private val scope: CoroutineScope, private val disposable: Disposable) : JPanel() {
  private val coursesTab: CoursesTab
  private val myCoursesProvider: MyCoursesProvider = MyCoursesProvider()
  private val sidePanel: CoursesProvidersSidePanel

  val languageSettings: LanguageSettings<*>? get() = coursesTab.languageSettings()

  init {
    layout = BorderLayout()
    coursesTab = CoursesTab()
    sidePanel = CoursesProvidersSidePanel(myCoursesProvider, disposable).apply {
      addTreeSelectionListener(CoursesProviderSelectionListener())
    }

    add(sidePanel, BorderLayout.WEST)
    add(coursesTab, BorderLayout.CENTER)
    preferredSize = JBUI.size(PANEL_WIDTH, PANEL_HEIGHT)
  }

  fun doValidation() {
    coursesTab.doValidation()
  }

  fun setSidePaneBackground() {
    UIUtil.setBackgroundRecursively(sidePanel, UIUtil.SIDE_PANEL_BACKGROUND)
  }

  fun loadCourses() {
    coursesTab.loadCourses(scope)
  }

  private inner class CoursesProviderSelectionListener : TreeSelectionListener {
    override fun valueChanged(e: TreeSelectionEvent?) {
      val node = e?.path?.lastPathComponent as DefaultMutableTreeNode
      val provider = node.userObject as? CoursesPlatformProvider ?: return
      val coursesProviderName = provider.name
      coursesTab.showPanel(coursesProviderName)
      EduCounterUsageCollector.courseSelectionTabSelected(provider)
    }
  }

  private inner class CoursesTab : JPanel() {
    private val panels: MutableList<CoursesPanel> = mutableListOf()
    private var activeTabName: String? = null
    private val cardLayout = JBCardLayout()

    init {
      layout = cardLayout
      val providers = CoursesPlatformProviderFactory.allProviders
      providers.forEach { provider ->
        addPanel(provider)
      }

      addPanel(myCoursesProvider)
      showPanel(providers.first().name)
      val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
      connection.subscribe(CoursesStorageBase.COURSE_DELETED, object : CourseDeletedListener {
        override fun courseDeleted(course: JBACourseFromStorage) {
          panels.forEach {
            it.updateModelAfterCourseDeletedFromStorage(course)
          }
        }
      })
    }

    private fun addPanel(coursesPlatformProvider: CoursesPlatformProvider) {
      val panel = coursesPlatformProvider.createPanel(scope, disposable)
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

    fun languageSettings() = currentPanel.languageSettings

    private val currentPanel: CoursesPanel
      get() {
        activeTabName ?: error("Active tab name is null")
        val activeComponent = (layout as JBCardLayout).findComponentById(activeTabName)
        return activeComponent as CoursesPanel
      }
  }
}
