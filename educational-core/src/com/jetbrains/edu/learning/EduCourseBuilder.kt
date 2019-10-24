package com.jetbrains.edu.learning

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils.loadText
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.coursecreator.ui.showNewStudyItemDialog
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.ext.testDirs
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createDefaultFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.joinPaths
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import java.io.IOException

/**
 * The main interface provides courses creation for some language.
 *
 * @param Settings container type holds course project settings state
 */
interface EduCourseBuilder<Settings> {
  /**
   * Shows UI for new study item creation
   *
   * @param model some parameters for UI extracted from context where creating action was called
   * @param additionalPanels additional ui elements which should be shown while new study item creation
   *
   * @see com.jetbrains.edu.coursecreator.ui.CCItemPositionPanel
   *
   * @return properties for study item creation
   */
  fun showNewStudyItemUi(project: Project, model: NewStudyItemUiModel, additionalPanels: List<AdditionalPanel>): NewStudyItemInfo? {
    return showNewStudyItemDialog(project, model, additionalPanels)
  }

  /**
   * Creates content (including its directory or module) of new lesson in project
   *
   * @param project Parameter is used in Java and Kotlin plugins
   * @param lesson  [Lesson] to create content for. It's already properly initialized and added to course.
   * @return [VirtualFile] of created lesson
   */
  fun createLessonContent(project: Project, lesson: Lesson, parentDirectory: VirtualFile): VirtualFile? {
    val lessonDirectory = arrayOfNulls<VirtualFile>(1)
    runWriteAction {
      try {
        lessonDirectory[0] = VfsUtil.createDirectoryIfMissing(
          parentDirectory, lesson.name)
      }
      catch (e: IOException) {
        LOG.error("Failed to create lesson directory", e)
      }
    }
    return lessonDirectory[0]
  }

  /**
   * Creates content (including its directory or module) of new task in project
   *
   * @param task [Task] to create content for. It's already properly initialized and added to corresponding lesson.
   * @return [VirtualFile] of created task
   */
  fun createTaskContent(project: Project, task: Task, parentDirectory: VirtualFile): VirtualFile? {
    try {
      createTask(task, parentDirectory)
    }
    catch (e: IOException) {
      LOG.error("Failed to create task", e)
    }
    val taskDir = parentDirectory.findChild(task.name)
    if (!isUnitTestMode) {
      refreshProject(project)
    }
    return taskDir
  }

  /**
   * Allows to update project modules and the whole project structure
   */
  fun refreshProject(project: Project) {
    refreshProject(project, null)
  }

  fun refreshProject(project: Project, listener: ProjectRefreshListener?) {
    listener?.onSuccess()
  }

  fun createInitialLesson(project: Project, course: Course): Lesson? {
    val lesson = CCCreateLesson().createAndInitItem(project, course, null, NewStudyItemInfo(EduNames.LESSON + 1, 1))
    val task = CCCreateTask().createAndInitItem(project, course, lesson, NewStudyItemInfo(EduNames.TASK + 1, 1))
    if (task != null) {
      lesson.addTask(task)
    }
    return lesson
  }

  /**
   * Add initial content for a new task: task and tests files if the corresponding files don't exist.
   * Supposed to use in course creator mode
   *
   * @param task initializing task
   */
  fun initNewTask(project: Project, lesson: Lesson, task: Task, info: NewStudyItemInfo) {
    if (task.taskFiles.isEmpty()) {
      val sourceDir = task.sourceDir
      val taskFile = TaskFile()
      val taskTemplateName = taskTemplateName
      if (taskTemplateName != null) {
        taskFile.name = joinPaths(sourceDir, taskTemplateName)
        taskFile.setText(StringUtil.notNullize(getInternalTemplateText(taskTemplateName)))
      }
      else {
        val (name, text) = createDefaultFile(task.lesson.course, "Task", "type task text here")
        taskFile.name = joinPaths(sourceDir, name)
        taskFile.setText(text)
      }
      task.addTaskFile(taskFile)
      val defaultTestFile = createDefaultTestFile(task)
      if (defaultTestFile != null) {
        task.addTaskFile(defaultTestFile)
      }
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
      return loadText(file)
    }
    catch (e: IOException) {
      LOG.error("Can't load text for `${taskFile.name}` task file", e)
    }
    return null
  }

  fun createDefaultTestFile(task: Task): TaskFile? {
    val testDirs = task.testDirs
    var testDir = ""
    if (!testDirs.isEmpty()) {
      testDir = testDirs[0]
    }
    val testTemplateName = testTemplateName
    if (testTemplateName != null) {
      val testText = getInternalTemplateText(testTemplateName)
      val test = TaskFile(joinPaths(testDir, testTemplateName), testText)
      test.isVisible = false
      return test
    }
    return null
  }

  val taskTemplateName: String? get() = null
  val testTemplateName: String? get() = null

  /**
   * @return object responsible for language settings
   * @see LanguageSettings
   */
  fun getLanguageSettings(): LanguageSettings<Settings>

  fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<Settings>? = null

  interface ProjectRefreshListener {
    fun onSuccess() {}
    fun onFailure(errorMessage: String) {}
  }

  companion object {
    val LOG = Logger.getInstance(EduCourseBuilder::class.java)
  }
}
