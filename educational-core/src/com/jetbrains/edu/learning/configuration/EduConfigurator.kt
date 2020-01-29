package com.jetbrains.edu.learning.configuration

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.EmptyIcon
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.isConfigFile
import org.jetbrains.annotations.SystemIndependent
import javax.swing.Icon
import javax.swing.JPanel

/**
 * The main interface provides courses support for some language and course type.
 *
 * IdeDefaultCourseTypes.kt provides default CourseTypeData. When creating new course, default course type will be suggested first,
 * or, if default course type is not specified, course types will be shown in alphabetic order.
 *
 * @see com.jetbrains.edu.coursecreator.ui.CCNewCoursePanel
 * @see com.jetbrains.edu.coursecreator.IdeDefaultCourseTypes
 *
 * To get configurator instance for some language use {@link EduConfiguratorManager}
 * and {@link EduConfiguratorManager} supports the corresponding filtering.
 *
 *
 * @param <Settings> container type holds course project settings state
 *
 * @see EduConfiguratorManager
 * @see EducationalExtensionPoint
 */
interface EduConfigurator<Settings> {
  val courseBuilder: EduCourseBuilder<Settings>
  val testFileName: String
  val taskCheckerProvider: TaskCheckerProvider

  /**
   * Used in educator plugin to filter files to be packed into course archive
   */
  fun excludeFromArchive(project: Project, file: VirtualFile): Boolean {
    val ancestorNames = mutableListOf<String>()
    var parent: VirtualFile? = file
    while (parent != null) {
      ancestorNames.add(parent.name)
      parent = parent.parent
    }

    val name = file.name
    // Project related files
    return ancestorNames.contains(Project.DIRECTORY_STORE_FOLDER) || "iml" == file.extension ||
           // Course structure files
           EduUtils.isTaskDescriptionFile(name) || isConfigFile(file) ||
           // Hidden files
           ancestorNames.any { ancestorName: String -> ancestorName.startsWith(".") } ||
           // Special files
           ancestorNames.contains(CCUtils.GENERATED_FILES_FOLDER) || EduNames.HINTS == name || EduNames.STEPIK_IDS_JSON == name
  }

  /**
   * @return true for all the test files
   */
  fun isTestFile(project: Project, file: VirtualFile): Boolean {
    if (file.isDirectory) return false
    val task = EduUtils.getTaskForFile(project, file) ?: return false
    val taskDir = task.getTaskDir(project) ?: return false
    return ContainerUtil.find(task.findTestDirs(taskDir)) { VfsUtilCore.isAncestor(it, file, true) } != null
  }

  /**
   * Provides directory path where task files should be placed in task folder.
   * Can be empty.
   *
   * For example, task folder is `courseName/lesson1/task1` and `getSourceDir` returns `src`
   * then any task files should be placed in `courseName/lesson1/task1/src` folder
   *
   * @return task files directory path
   */
  val sourceDir: @SystemIndependent String
    get() = ""

  /**
   * Provides list of directories where test files should be placed in task folder.
   * Can be empty.
   *
   * See [EduConfigurator.sourceDir] javadoc for example.
   *
   * @return list of test files directories paths
   */
  val testDirs: List<@SystemIndependent String>
    get() = listOf()

  /**
   * Allows to determine if configurator can be used in current environment or not.
   *
   * @return true if configurator can be used, false otherwise
   *
   * @see EduConfiguratorManager
   */
  val isEnabled: Boolean
    get() = true

  /**
   * Allows to determine if configurator can be used to create new course using course creator features.
   *
   * @return true if configurator can be used to create new courses
   */
  val isCourseCreatorEnabled: Boolean
    get() = true

  /**
   * Constructs file name for Stepik tasks according to its text.
   * For example, Java requires file name should be the same as name of public class in it
   *
   * @see com.jetbrains.edu.learning.stepik.StepikTaskBuilder
   */
  fun getMockFileName(text: String): String? = null

  /**
   * Allows to customize file template used as playground in theory and choice tasks
   * Template should work along with the according [com.jetbrains.edu.learning.checker.TheoryTaskChecker]
   *
   * @see com.jetbrains.edu.learning.stepik.StepikTaskBuilder
   */
  val mockTemplate: String
    get() = ""

  /**
   * Provide IDE plugin ids which are required for correct work of courses for the corresponding language.
   *
   * @return list of plugin ids
   */
  val pluginRequirements: List<String>
    get() = listOf()

  /**
   * Allows to perform heavy computations (ex.HTTP requests) before actual project is created
   * It's recommended to perform these computations under progress
   * @throws CourseCantBeStartedException if impossible to start course
   */
  fun beforeCourseStarted(course: Course) {
  }

  /**
   * This icon is used in places where course is associated with language.
   * For example, 'Browse Courses' and 'Create New Course' dialogs.
   *
   * @return 16x16 icon
   *
   * @see com.jetbrains.edu.learning.newproject.ui.CoursesPanel
   * @see com.jetbrains.edu.coursecreator.ui.CCNewCoursePanel
   */
  val logo: Icon
    get() = EmptyIcon.ICON_16

  /**
   * Tab next to the task description.
   *
   * @see com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
   */
  fun additionalTaskTab(currentTask: Task?, project: Project): Pair<JPanel, String>? = null
}