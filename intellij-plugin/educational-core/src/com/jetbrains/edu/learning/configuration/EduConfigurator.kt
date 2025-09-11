package com.jetbrains.edu.learning.configuration

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.profile.codeInspection.PROFILE_DIR
import com.intellij.util.ui.EmptyIcon
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProviderEP
import com.jetbrains.edu.learning.configuration.attributesEvaluator.AttributesBuilderContext
import com.jetbrains.edu.learning.configuration.attributesEvaluator.AttributesEvaluator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat.Companion.taskDescriptionRegex
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.loadMarketplaceCourseStructure
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.toCourseInfoHolder
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.isLocalConfigFileName
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.isRemoteConfigFileName
import org.jetbrains.annotations.SystemIndependent
import javax.swing.Icon

private val NAME_START_WITH_DOT_REGEX = """^\..*$""".toRegex()

private fun AttributesBuilderContext.legacyExcludeFromArchiveHiddenFilesAndDirectories() = name(NAME_START_WITH_DOT_REGEX) {
  excludeFromArchive()

  any {
    excludeFromArchive()
  }
}

private val ROOT_COURSE_ATTRIBUTES_EVALUATOR = AttributesEvaluator {
  legacyExcludeFromArchiveHiddenFilesAndDirectories()

  // .idea folder
  dir(Project.DIRECTORY_STORE_FOLDER) {
    includeIntoArchive()

    dirAndChildren(PROFILE_DIR, "scopes") {
      includeIntoArchive()
    }

    legacyExcludeFromArchiveHiddenFilesAndDirectories()

    // https://intellij-support.jetbrains.com/hc/en-us/articles/206544839-How-to-manage-projects-under-Version-Control-Systems
    file("workspace.xml", "usage.statistics.xml") {
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }
    dirAndChildren("shelf") {
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }
  }

  // do not automatically include hidden files and directories (.file, .directory/) to archive
  name(NAME_START_WITH_DOT_REGEX) {
    archiveInclusionPolicy(ArchiveInclusionPolicy.EXCLUDED_BY_DEFAULT)
    any {
      archiveInclusionPolicy(ArchiveInclusionPolicy.EXCLUDED_BY_DEFAULT)
    }
  }

  extension("iml") {
    excludeFromArchive()
    archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    courseViewVisibility(CourseViewVisibility.INVISIBLE_FOR_ALL)
  }

  file(taskDescriptionRegex) {
    excludeFromArchive()
    archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
  }

  file(pred { isLocalConfigFileName(it) || isRemoteConfigFileName(it) }) {
    excludeFromArchive()
    archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
  }

  dirAndChildren(CCUtils.GENERATED_FILES_FOLDER, direct = true) {
    excludeFromArchive()
    archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    courseViewVisibility(CourseViewVisibility.INVISIBLE_FOR_ALL)
  }

  file(EduNames.COURSE_IGNORE, EduFormatNames.COURSE_ICON_FILE) {
    excludeFromArchive()
    archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
  }

  // legacy files
  file(EduNames.HINTS, EduNames.STEPIK_IDS_JSON) {
    excludeFromArchive()
  }

  dirAndChildren(EduNames.VCS_GIT) {
    archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    courseViewVisibility(CourseViewVisibility.INVISIBLE_FOR_ALL)
  }
}

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
 * @param Settings container type holds course project settings state
 *
 * @see EduConfiguratorManager
 * @see EducationalExtensionPoint
 *
 * If you want to advertise courses supported by the configurator even when
 * required plugins are not installed, implement [CourseCompatibilityProvider] and register it via [CourseCompatibilityProviderEP]
 *
 * If you add any new methods here, please do not forget to add it also to
 * @see com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
 */
interface EduConfigurator<Settings : EduProjectSettings> {
  val courseBuilder: EduCourseBuilder<Settings>
  val testFileName: String
  val taskCheckerProvider: TaskCheckerProvider

  val courseFileAttributesEvaluator: AttributesEvaluator
    get() = ROOT_COURSE_ATTRIBUTES_EVALUATOR

  /**
   * @return true for all the test files
   */
  fun isTestFile(task: Task, path: String): Boolean {
    return path == testFileName || testDirs.any { testDir -> VfsUtilCore.isEqualOrAncestor(testDir, path) }
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
  fun getMockFileName(course: Course, text: String): String? = courseBuilder.mainTemplateName(course)

  /**
   * Allows to customize file template used as playground in theory and choice tasks
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
  val pluginRequirements: List<PluginId>
    get() = listOf()

  /**
   * Allows to perform heavy computations (ex.HTTP requests) before actual project is created
   * It's recommended to perform these computations under progress
   * @throws CourseCantBeStartedException if impossible to start course
   */
  fun beforeCourseStarted(course: Course) {
    course.loadMarketplaceCourseStructure()
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

  val defaultPlaceholderText: String
    get() = CCUtils.DEFAULT_PLACEHOLDER_TEXT

  fun getCodeTaskFile(project: Project, task: Task): TaskFile? = task.getCodeTaskFile(project)

  fun getEnvironmentSettings(project: Project): Map<String, String> = mapOf()
}

fun EduConfigurator<*>.excludeFromArchive(project: Project, file: VirtualFile): Boolean =
  courseFileAttributes(project, file).excludedFromArchive

fun EduConfigurator<*>.excludeFromArchive(holder: CourseInfoHolder<out Course?>, file: VirtualFile): Boolean =
  courseFileAttributes(holder, file).excludedFromArchive

fun EduConfigurator<*>.courseFileAttributes(project: Project, file: VirtualFile): CourseFileAttributes =
  courseFileAttributes(project.toCourseInfoHolder(), file)

fun EduConfigurator<*>.courseFileAttributes(holder: CourseInfoHolder<out Course?>, file: VirtualFile): CourseFileAttributes =
  courseFileAttributesEvaluator.attributesForFile(holder, file)