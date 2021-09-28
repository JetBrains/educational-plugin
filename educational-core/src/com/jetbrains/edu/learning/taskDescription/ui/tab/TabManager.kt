package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.jetbrains.edu.coursecreator.ui.YamlHelpTab
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.TheoryTab
import com.jetbrains.edu.learning.stepik.hyperskill.TopicsTab
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.getRelatedTheoryTask
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.SubmissionsTab
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager.TabType.*
import org.jetbrains.annotations.PropertyKey


class TabManager(private val project: Project, private val contentManager: ContentManager) {

  fun updateTabs(task: Task?) {
    val selectedTab = getSelectedTab()
    removeAdditionalTabs()
    if (task == null) return

    val tabStructure = task.tabsToBeDisplayed()
    tabStructure.forEachIndexed { index, tabType ->
      // +1 here because of Description tab which is unhandled here for now (see EDU-4267)
      updateTab(tabType, task, index + 1)
    }
    if (selectedTab != null) {
      ApplicationManager.getApplication().invokeLater {
        // This hack is needed for Swing panels, because otherwise when tab isn't initialized yet and we trying to select it
        // - it leads to exceptions and unexpected behavior
        selectTab(selectedTab)
      }
    }
  }

  private fun removeAdditionalTabs() {
    contentManager.contents.forEach { content ->
      /**
       * Additional tabs could be found in contentManager via their types
       * [com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager.TabType], while Description tab is still handled via it's name.
       * Hack, to be removed a bit later
       */
      if (content.displayName != EduCoreBundle.message("label.description")) {
        contentManager.removeContent(content, true)
      }
    }
  }

  private fun createTheoryTab(theoryTask: TheoryTask): TheoryTab {
    theoryTask.course as? HyperskillCourse ?: error("Theory tab is designed for Hyperskill course, but task is located in different course")
    return TheoryTab(project, theoryTask)
  }

  private fun createTopicsTab(task: Task): TopicsTab {
    val course = task.course as? HyperskillCourse
                 ?: error("Topics tab is designed for Hyperskill course, but task is located in different course")
    return TopicsTab(project, course, task)
  }

  private fun createSubmissionsTab(task: Task): SubmissionsTab = SubmissionsTab.create(project, task)

  private fun createYamlHelpTab(): YamlHelpTab = YamlHelpTab(project)

  private fun addTab(additionalTab: AdditionalTab, tabIndex: Int) {
    val tabContent = additionalTab.createContent()
    tabContent.isCloseable = false
    contentManager.addContent(tabContent, tabIndex)
  }

  private fun createTab(tabType: TabType, task: Task): AdditionalTab? = when (tabType) {
    THEORY_TAB -> {
      val theoryTask = task.getRelatedTheoryTask()
      if (theoryTask != null) createTheoryTab(theoryTask) else null
    }
    TOPICS_TAB -> createTopicsTab(task)
    SUBMISSIONS_TAB -> createSubmissionsTab(task)
    YAML_HELP_TAB -> createYamlHelpTab()
  }

  fun getContent(tabType: TabType): Content? {
    return contentManager.contents
      .find { content ->
        val panel = content.component as? AdditionalTab
        panel?.tabType == tabType
      }
  }

  fun updateTab(tabType: TabType, task: Task?, index: Int? = null) {
    if (task == null) return

    removeTab(tabType)
    val tabIndex = index ?: getTabIndex(tabType, task) ?: return
    val newTab = createTab(tabType, task) ?: return
    addTab(newTab, tabIndex)
  }

  private fun removeTab(tabType: TabType) {
    val tabContent = getContent(tabType) ?: return
    contentManager.removeContent(tabContent, true)
  }

  private fun Task.tabsToBeDisplayed(): List<TabType> {
    val result = mutableListOf<TabType>()
    val course = course

    if (course.isStudy) {
      if (course is HyperskillCourse && course.isTaskInProject(this)) {
        result.add(TOPICS_TAB)
      }
      if (supportSubmissions() && SubmissionsManager.getInstance(project).submissionsSupported()) {
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

  private fun getSelectedTab(): TabType? {
    val currentContent = contentManager.selectedContent?.component as? AdditionalTab ?: return null
    return currentContent.tabType
  }

  fun selectTab(tabType: TabType) {
    val content = getContent(tabType) ?: return
    if (content in contentManager.contents) {
      contentManager.setSelectedContent(content)
    }
  }

  enum class TabType(@PropertyKey(resourceBundle = BUNDLE) private val nameId: String) {
    THEORY_TAB("hyperskill.theory.tab.name"),
    TOPICS_TAB("hyperskill.topics.tab.name"),
    SUBMISSIONS_TAB("submissions.tab.name"),
    YAML_HELP_TAB("yaml.help.tab.name");

    val tabName: String get() = EduCoreBundle.message(nameId)
  }
}