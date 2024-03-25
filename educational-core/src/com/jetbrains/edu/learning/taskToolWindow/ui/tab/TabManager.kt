package com.jetbrains.edu.learning.taskToolWindow.ui.tab

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi.TabPanel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.util.minimumHeight
import com.intellij.ui.util.preferredHeight
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.xml.ui.EmptyPane
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.getRelatedTheoryTask
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.*
import org.jetbrains.plugins.notebooks.visualization.r.inlays.components.EmptyComponentPanel
import java.awt.Insets
import java.awt.event.ContainerEvent
import java.awt.event.ContainerListener
import javax.swing.JPanel
import javax.swing.plaf.basic.BasicTabbedPaneUI
import javax.swing.plaf.basic.BasicTabbedPaneUI.TabbedPaneLayout


class TabManager(private val project: Project) : Disposable {

  val tabbedPane: JBTabbedPane = JBTabbedPane()
  val descriptionTab = (DESCRIPTION_TAB.createTab(project) as DescriptionTab)

  init {
    tabbedPane.font = JBFont.medium().biggerOn(1.0f)
    tabbedPane.addTab(DESCRIPTION_TAB.tabName, descriptionTab)
//    tabbedPane.tabComponentInsets = JBUI.emptyInsets()
    getTab(DESCRIPTION_TAB).border = JBEmptyBorder(0)
  }

  fun updateTabs(task: Task?) {
    removeAdditionalTabs()
    if (task == null) return

    task.tabsToBeDisplayed().forEach { updateTab(it, task) }
  }

  override fun dispose() {}

  fun getTab(tabType: TabType): TaskToolWindowTab {
    val indexOfTab = tabbedPane.indexOfTab(tabType.tabName)
    if (indexOfTab == -1) {
      tabbedPane.addTab(tabType.tabName, tabType.createTab(project))
      val newIndexOfTab = tabbedPane.indexOfTab(tabType.tabName)
      val taskToolWindowTab = tabbedPane.getComponentAt(newIndexOfTab) as TaskToolWindowTab
      taskToolWindowTab.border = JBEmptyBorder(0)
      return taskToolWindowTab
    }
    return tabbedPane.getComponentAt(indexOfTab) as TaskToolWindowTab
  }


  fun updateTab(tabType: TabType, task: Task) {
    if (tabType !in task.tabsToBeDisplayed()) {
      val indexOfTab = tabbedPane.indexOfTab(tabType.tabName)
      if (indexOfTab > 0) tabbedPane.removeTabAt(indexOfTab)
      return
    }

    val taskForUpdate = if (tabType == THEORY_TAB) task.getRelatedTheoryTask() ?: return else task

    val tab = getTab(tabType)
    tab.update(taskForUpdate)
  }

  private fun removeAdditionalTabs() {
    TabType.values().forEach {
      val indexOfTab = tabbedPane.indexOfTab(it.tabName)
      if (indexOfTab > 0) {
        tabbedPane.removeTabAt(indexOfTab)
      }
    }

  }

  private fun Task.tabsToBeDisplayed(): List<TabType> {
    val result = mutableListOf(DESCRIPTION_TAB)
    val course = course

    if (course.isStudy) {
      if (course is HyperskillCourse && course.isTaskInProject(this)) {
        result.add(TOPICS_TAB)
      }
      if (supportSubmissions && SubmissionsManager.getInstance(project).submissionsSupported()) {
        result.add(SUBMISSIONS_TAB)
      }
      if (course is HyperskillCourse && course.isTaskInTopicsSection(this) && this !is TheoryTask) {
        result.add(THEORY_TAB)
      }
    }

    return result
  }

  fun selectTab(tabType: TabType) {
    val indexOfTab = tabbedPane.indexOfTab(tabType.tabName)
    if (indexOfTab == -1) {
      tabbedPane.selectedIndex = 0
      return
    }
    tabbedPane.selectedIndex = indexOfTab
  }

  fun updateTaskSpecificPanel(task: Task?) {
    descriptionTab.updateTaskSpecificPanel(task)
  }

  fun updateTaskDescription(task: Task?) {
    task ?: return
    descriptionTab.update(task)
  }
}
