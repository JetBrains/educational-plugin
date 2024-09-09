package com.jetbrains.edu.learning.taskToolWindow.ui.tab

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.stepik.hyperskill.TheoryTab
import com.jetbrains.edu.learning.stepik.hyperskill.TopicsTab
import com.jetbrains.edu.learning.stepik.hyperskill.getRelatedTheoryTask
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.SubmissionsTab
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.*


class TabManager(private val project: Project) : Disposable {

  val tabbedPane: JBTabbedPane = JBTabbedPane().apply {
    font = JBFont.medium().biggerOn(1.0f)
    border = JBUI.Borders.customLineBottom(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
  }
  val descriptionTab = createTab(DESCRIPTION_TAB) as DescriptionTab

  fun updateTabs(task: Task?) {
    removeAdditionalTabs()
    if (task == null) return

    task.tabsToBeDisplayed().forEach { updateTab(it, task) }
  }

  override fun dispose() {}

  private fun getOrCreateTab(tabType: TabType): TaskToolWindowTab = getTab(tabType) ?: createTab(tabType)

  fun getTab(tabType: TabType): TaskToolWindowTab? {
    val indexOfTab = tabbedPane.indexOfTab(tabType.tabName)
    if (indexOfTab == -1) {
      return null
    }
    return tabbedPane.getComponentAt(indexOfTab) as TaskToolWindowTab
  }

  private fun createTab(tabType: TabType): TaskToolWindowTab {
    val taskToolWindowTab = when (tabType) {
      DESCRIPTION_TAB -> DescriptionTab(project)
      THEORY_TAB -> TheoryTab(project)
      TOPICS_TAB -> TopicsTab(project)
      SUBMISSIONS_TAB -> SubmissionsTab(project)
    }
    Disposer.register(this, taskToolWindowTab)
    tabbedPane.addTab(tabType.tabName, taskToolWindowTab)
    return taskToolWindowTab
  }

  fun updateTab(tabType: TabType, task: Task) {
    if (tabType !in task.tabsToBeDisplayed()) {
      removeTab(tabType)
      return
    }

    val taskForUpdate = if (tabType == THEORY_TAB) task.getRelatedTheoryTask() ?: return else task

    val tab = getOrCreateTab(tabType)
    tab.update(taskForUpdate)
  }

  private fun removeTab(tabType: TabType) {
    val tab = getTab(tabType) ?: return
    tabbedPane.remove(tab)
    Disposer.dispose(tab)
  }

  private fun removeAdditionalTabs() = TabType.values()
    .filter { it != DESCRIPTION_TAB }
    .forEach { removeTab(it) }

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
    tabbedPane.selectedIndex = if (indexOfTab != -1) indexOfTab else 0
  }

  fun updateTaskSpecificPanel(task: Task?) {
    descriptionTab.updateTaskSpecificPanel(task)
  }

  fun updateTaskDescription(task: Task?) {
    task ?: return
    descriptionTab.update(task)
  }
}
