package com.jetbrains.edu.learning.taskToolWindow.ui.tab

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.getRelatedTheoryTask
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.*


class TabManager(private val project: Project) : Disposable {

  val tabbedPane: JBTabbedPane = JBTabbedPane()
  val descriptionTab = DESCRIPTION_TAB.createTab(project) as DescriptionTab

  init {
    tabbedPane.font = JBFont.medium().biggerOn(1.0f)
    tabbedPane.addTab(DESCRIPTION_TAB.tabName, descriptionTab)
    // set border for the tab container and register in Disposer
    val descriptionTab = getTab(DESCRIPTION_TAB)
    Disposer.register(this, descriptionTab)
    descriptionTab.border = JBEmptyBorder(0)
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
      // set border for the tab container
      val taskToolWindowTab = tabbedPane.getComponentAt(newIndexOfTab) as TaskToolWindowTab
      taskToolWindowTab.border = JBEmptyBorder(0)
      Disposer.register(this, taskToolWindowTab)
      return taskToolWindowTab
    }
    return tabbedPane.getComponentAt(indexOfTab) as TaskToolWindowTab
  }


  fun updateTab(tabType: TabType, task: Task) {
    if (tabType !in task.tabsToBeDisplayed()) {
      val indexOfTab = tabbedPane.indexOfTab(tabType.tabName)
      if (indexOfTab > 0) {
        val tabToRemove = tabbedPane.getComponentAt(indexOfTab) as TaskToolWindowTab
        tabbedPane.removeTabAt(indexOfTab)
        Disposer.dispose(tabToRemove)
      }
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
