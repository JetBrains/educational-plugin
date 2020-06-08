package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTopic
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillTaskCheckerProvider
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillCourseBuilder
import com.jetbrains.edu.learning.taskDescription.ui.AdditionalTabPanel
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import javax.swing.Icon

/**
 * Hyperskill contractors edit existing Hyperskill projects as Stepik lessons.
 * These lessons don't have language/environment inside, so we need to detect them.
 *
 * @see com.jetbrains.edu.coursecreator.actions.stepik.hyperskill.GetHyperskillLesson
 */
abstract class HyperskillConfigurator<T>(private val baseConfigurator: EduConfigurator<T>) : EduConfigurator<T> {

  override val taskCheckerProvider: TaskCheckerProvider
    get() = HyperskillTaskCheckerProvider(baseConfigurator.taskCheckerProvider)

  override val courseBuilder: EduCourseBuilder<T>
    get() = HyperskillCourseBuilder(baseConfigurator.courseBuilder)

  override fun additionalTaskTabs(currentTask: Task?, project: Project): List<AdditionalTabPanel> {
    val additionalTabs = super.additionalTaskTabs(currentTask, project)
    val topicsTab = getTopicsTab(currentTask, project) ?: return additionalTabs
    val additionalTabsWithTopics = mutableListOf(topicsTab)
    additionalTabsWithTopics.addAll(additionalTabs)
    return additionalTabsWithTopics
  }

  private fun getTopicsTab(currentTask: Task?, project: Project): AdditionalTabPanel? {
    if (currentTask == null) return null
    val course = currentTask.lesson.course
    if (course is HyperskillCourse && course.isStudy) {
      if (!course.isTaskInProject(currentTask)) return null
      val topicsPanel = AdditionalTabPanel(project, TOPICS_TAB_NAME)
      topicsPanel.addHyperlinkListener(EduBrowserHyperlinkListener.INSTANCE)

      val topics = course.taskToTopics[currentTask.index - 1]
      var descriptionText = "<h3 ${StyleManager().textStyleHeader}>Topics for current stage :</h3>"
      if (topics != null) {
        for (topic in topics) {
          descriptionText += topicLink(topic)
          descriptionText += "<br>"
        }
      }
      else {
        descriptionText += "<a ${StyleManager().textStyleHeader}>No topics found for current stage."
      }
      topicsPanel.setText(descriptionText)

      return topicsPanel
    }
    return null
  }

  private fun topicLink(topic: HyperskillTopic): String =
    "<a ${StyleManager().textStyleHeader};color:${linkColor()} href=\"https://hyperskill.org/learn/step/${topic.theoryId}/\">${topic.title}</a>"

  private fun linkColor(): String = if (UIUtil.isUnderDarcula()) "#6894C6" else "#5C84C9"

  /**
   * We have to do this stuff because implementation by delegation still works unstable
   */
  override val testFileName: String
    get() = baseConfigurator.testFileName

  override val sourceDir: String
    get() = baseConfigurator.sourceDir

  override val testDirs: List<String>
    get() = baseConfigurator.testDirs

  override val isEnabled: Boolean
    get() = baseConfigurator.isEnabled

  override val isCourseCreatorEnabled: Boolean
    get() = baseConfigurator.isCourseCreatorEnabled

  override val mockTemplate: String
    get() = baseConfigurator.mockTemplate

  override val pluginRequirements: List<String>
    get() = baseConfigurator.pluginRequirements

  override val logo: Icon
    get() = baseConfigurator.logo

  override fun excludeFromArchive(project: Project, file: VirtualFile): Boolean = baseConfigurator.excludeFromArchive(project, file)
  override fun isTestFile(project: Project, file: VirtualFile): Boolean = baseConfigurator.isTestFile(project, file)
  override fun getMockFileName(text: String): String? = baseConfigurator.getMockFileName(text)
  override fun beforeCourseStarted(course: Course) = baseConfigurator.beforeCourseStarted(course)

  companion object {
    const val HYPERSKILL_TEST_DIR = "hstest"
    const val TOPICS_TAB_NAME = "Topics"
  }
}
