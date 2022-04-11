package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.util.containers.enumMapOf
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.getRelatedTheoryTask
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.*


class TabManagerImpl(private val project: Project, private val contentManager: ContentManager) : TabManager {
  private val tabs: MutableMap<TabType, AdditionalTab> = enumMapOf()

  override fun updateTabs(task: Task?) {
    removeAdditionalTabs()
    if (task == null) return

    val tabStructure = task.tabsToBeDisplayed()
    tabStructure.forEachIndexed { index, tabType ->
      // +1 here because of Description tab which is unhandled here for now (see EDU-4267)
      updateTab(tabType, task, index + 1)
    }
  }

  override fun dispose() {}

  private fun getContent(tabType: TabType): Content = getTab(tabType).content

  override fun getTab(tabType: TabType): AdditionalTab {
    return tabs.getOrPut(tabType) {
      tabType.createTab(project).also { tab ->
        Disposer.register(this, tab)
      }
    }
  }

  override fun isShowing(tabType: TabType): Boolean {
    val content = getContent(tabType)
    return content in contentManager.contents
  }

  override fun updateTab(tabType: TabType, task: Task?) {
    if (task == null) return

    val tabIndex = getTabIndex(tabType, task)
    if (tabIndex == null) {
      LOG.warn("Trying to update $tabType that isn't shown for such task $task")
      return
    }
    return updateTab(tabType, task, tabIndex)
  }

  private fun updateTab(tabType: TabType, task: Task, index: Int) {
    if (index > contentManager.contentCount) {
      error("Index calculated for $tabType tab is out of contentManager's contents range")
    }

    val taskForUpdate = if (tabType == THEORY_TAB) {
      task.getRelatedTheoryTask() ?: return
    }
    else {
      task
    }

    val tab = getTab(tabType)
    tab.update(taskForUpdate)

    // not sure about this, but seems like it works well
    contentManager.addContent(tab.content, index)
  }

  private fun removeAdditionalTabs() {
    contentManager.contents.forEach { content ->
      if (!content.isDescriptionTab()) {
        contentManager.removeContent(content, false)
      }
    }
  }

  private fun Task.tabsToBeDisplayed(): List<TabType> {
    val result = mutableListOf<TabType>()
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
    else {
      result.add(YAML_HELP_TAB)
    }

    return result
  }

  private fun getTabIndex(tabType: TabType, task: Task): Int? {
    // +1 here because of Description tab which is unhandled here for now (see EDU-4267)
    val tabIndex = task.tabsToBeDisplayed().indexOf(tabType) + 1
    return if (tabIndex == 0) null else tabIndex
  }

  override fun selectTab(tabType: TabType) {
    val content = getContent(tabType)
    if (content in contentManager.contents) {
      contentManager.setSelectedContent(content, true)
    }
    else {
      val descriptionTab = contentManager.contents.find { it.isDescriptionTab() } ?: return
      contentManager.setSelectedContent(descriptionTab, true)
    }
  }

  private fun Content.isDescriptionTab(): Boolean {
    /**
     * Additional tabs could be found in contentManager via their types
     * [com.jetbrains.edu.learning.taskDescription.ui.tab.TabType], while Description tab is still handled via it's name.
     * Hack, to be removed a bit later
     */
    return displayName == EduCoreBundle.message("label.description")
  }

  companion object {
    private val LOG: Logger = logger<TabManager>()
  }
}