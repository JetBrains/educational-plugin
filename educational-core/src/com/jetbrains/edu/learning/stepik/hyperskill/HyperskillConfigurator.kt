package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillTaskCheckerProvider
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillCourseBuilder
import com.jetbrains.edu.learning.taskDescription.ui.AdditionalTabPanel
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

  override fun additionalTaskTab(currentTask: Task?, project: Project): AdditionalTabPanel? {
    if (currentTask == null) return null
    val course = currentTask.lesson.course
    if (course is HyperskillCourse && course.isStudy) {
      if (!course.isTaskInProject(currentTask)) return null
      return TopicsTabPanel(project, course, currentTask)
    }
    return null
  }

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
  override fun getMockFileName(text: String): String = baseConfigurator.getMockFileName(text)
  override fun getCodeTaskFile(project: Project, task: Task): TaskFile? = baseConfigurator.getCodeTaskFile(project, task)
  override fun beforeCourseStarted(course: Course) = baseConfigurator.beforeCourseStarted(course)

  companion object {
    const val HYPERSKILL_TEST_DIR = "hstest"
  }
}
