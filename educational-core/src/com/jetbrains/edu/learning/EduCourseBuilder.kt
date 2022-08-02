package com.jetbrains.edu.learning

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateTask
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.showNewStudyItemDialog
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.LESSON
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.testDirs
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTaskBase.Companion.INPUT_PATTERN_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTaskBase.Companion.OUTPUT_PATTERN_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.joinPaths
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import org.jetbrains.annotations.Nls
import java.io.IOException

/**
 * The main interface provides courses creation for some language.
 *
 * @param Settings container type holds course project settings state
 *
 * If you add any new methods here, please do not forget to add it also to
 * @see com.jetbrains.edu.learning.stepik.hyperskill.HyperskillCourseBuilder
 */
interface EduCourseBuilder<Settings : Any> {
  /**
   * Shows UI for new study item creation
   *
   * @param model some parameters for UI extracted from context where creating action was called
   *
   * @return properties for study item creation
   */
  fun showNewStudyItemUi(
    project: Project,
    course: Course,
    model: NewStudyItemUiModel,
    studyItemCreator: (NewStudyItemInfo) -> Unit
  ) = showNewStudyItemDialog(project, course, model, studyItemCreator = studyItemCreator)

  /**
   * Creates additional content of new study item in project
   * [item] object is already properly initialized and added to corresponding parent.
   */
  fun onStudyItemCreation(project: Project, item: StudyItem) {}

  /**
   * Allows to update project modules and the whole project structure
   */
  fun refreshProject(project: Project, cause: RefreshCause) { }

  fun createInitialLesson(project: Project, course: Course): Lesson? {
    val lessonInfo = NewStudyItemInfo(LESSON + 1, 1, ::Lesson)
    val lesson = CCCreateLesson().createAndInitItem(project, course, course, lessonInfo)
    val taskInfo = NewStudyItemInfo(TASK + 1, 1, ::EduTask)
    val task = CCCreateTask().createAndInitItem(project, course, lesson, taskInfo)
    lesson.addTask(task)
    return lesson
  }

  /**
   * Returns list of templates to create files in new task that supposed to be checked by tests launch.
   *
   * @param info general information about new task
   * @param withSources determines if returning template list should contain task source files (non test files).
   * Can be `false` if parent item is [FrameworkLesson]
   *
   * @see EduTask
   */
  fun getTestTaskTemplates(course: Course, info: NewStudyItemInfo, withSources: Boolean): List<TemplateFileInfo> =
    getDefaultTaskTemplates(course, info, withSources, true)

  /**
   * Returns list of templates to create files in new task that supposed to be run by user.
   * In compiled languages it means that task should have `main` function
   *
   * @param info general information about new task
   * @param withSources determines if returning template list should contain task source files (non test files).
   * Can be `false` if parent item is [FrameworkLesson]
   *
   * @see OutputTask
   * @see ChoiceTask
   * @see TheoryTask
   * @see IdeTask
   */
  fun getExecutableTaskTemplates(course: Course, info: NewStudyItemInfo, withSources: Boolean): List<TemplateFileInfo> =
    getDefaultTaskTemplates(course, info, withSources, false)

  /**
   * Returns default list of templates to create files in new task.
   *
   * If you need to specify different templates for test and executable tasks,
   * override [getTestTaskTemplates] and [getExecutableTaskTemplates] methods respectively.
   *
   * @param info general information about new task
   * @param withSources determines if returning template list should contain source files (non test files).
   * Can be `false` if parent item is [FrameworkLesson]
   * @param withTests determines if returning template list should contain test files
   */
  fun getDefaultTaskTemplates(
    course: Course,
    info: NewStudyItemInfo,
    withSources: Boolean,
    withTests: Boolean
  ): List<TemplateFileInfo> {
    val configurator = course.configurator ?: return emptyList()
    val templates = mutableListOf<TemplateFileInfo>()

    if (withSources) {
      val taskTemplate = if (withTests) taskTemplateName else mainTemplateName
      if (taskTemplate != null) {
        templates += TemplateFileInfo(taskTemplate, joinPaths(configurator.sourceDir, taskTemplate), true)
      }
    }

    if (withTests) {
      val testTemplate = testTemplateName
      if (testTemplate != null) {
        val testFileName = configurator.testFileName.takeIf { it.isNotEmpty() } ?: testTemplate
        templates += TemplateFileInfo(testTemplate, joinPaths(configurator.testDirs.firstOrNull(), testFileName), false)
      }
    }

    return templates
  }

  fun extractInitializationParams(project: Project, info: NewStudyItemInfo): Map<String, String> = emptyMap()

  /**
   * Add initial content for a new task: task and tests files if the corresponding files don't exist.
   * Supposed to use in course creator mode
   *
   * @param task initializing task
   */
  fun initNewTask(project: Project, course: Course, task: Task, info: NewStudyItemInfo, withSources: Boolean) {
    val templates = when (task) {
      is EduTask -> getTestTaskTemplates(course, info, withSources)
      is OutputTask -> {
        val outputTemplate = TemplateFileInfo(
          OUTPUT_PATTERN_NAME,
          joinPaths(course.testDirs.firstOrNull(), OUTPUT_PATTERN_NAME),
          false
        )
        val inputTemplate = TemplateFileInfo(
          INPUT_PATTERN_NAME,
          joinPaths(course.testDirs.firstOrNull(), INPUT_PATTERN_NAME),
          false
        )
        getExecutableTaskTemplates(course, info, withSources) + outputTemplate + inputTemplate
      }
      is ChoiceTask -> getExecutableTaskTemplates(course, info, withSources)
      is TheoryTask -> getExecutableTaskTemplates(course, info, withSources)
      is IdeTask -> getExecutableTaskTemplates(course, info, withSources)
      else -> return
    }

    val params = extractInitializationParams(project, info)

    for (template in templates) {
      val taskFile = template.toTaskFile(params)
      task.addTaskFile(taskFile)
    }
  }

  /**
   * Copy and return text of given `taskFile`.
   * Note: it is needed to override if content of copied text should be modified before copying.
   *
   * @param taskFile - `taskFile` which text will be copied.
   * @param taskDir  - directory of given `taskFile`.
   * @param newTask  - task for which the text is copied.
   * @return return copied text of given [taskFile] or null if can't load it.
   */
  fun getTextForNewTask(taskFile: TaskFile, taskDir: VirtualFile, newTask: Task): String? {
    try {
      val file = EduUtils.findTaskFileInDir(taskFile, taskDir)
      if (file == null) {
        LOG.warn("Can't find a file by path relative to this file for `${taskFile.name}` file")
        return null
      }
      return file.loadEncodedContent()
    }
    catch (e: IOException) {
      LOG.error("Can't load text for `${taskFile.name}` task file", e)
    }
    return null
  }

  val taskTemplateName: String? get() = null
  val mainTemplateName: String? get() = taskTemplateName
  val testTemplateName: String? get() = null

  /**
   * @return object responsible for language settings
   * @see LanguageSettings
   */
  fun getLanguageSettings(): LanguageSettings<Settings>

  fun getSupportedLanguageVersions(): List<String> = emptyList()

  fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<Settings>? = null

  /**
   * Validates a study item name.
   * Returns null if [name] is a correct item name, otherwise returns error message string.
   *
   * @see [com.jetbrains.edu.coursecreator.CCStudyItemPathInputValidator]
   */
  @Nls(capitalization = Nls.Capitalization.Sentence)
  fun validateItemName(project: Project, name: String, itemType: StudyItemType): String? {
    return null
  }

  /**
   * Provides language specific action before study item deletion.
   * Called in write action and only in educator mode
   */
  fun beforeStudyItemDeletion(project: Project, item: StudyItem) {}

  companion object {
    val LOG = Logger.getInstance(EduCourseBuilder::class.java)
  }
}
