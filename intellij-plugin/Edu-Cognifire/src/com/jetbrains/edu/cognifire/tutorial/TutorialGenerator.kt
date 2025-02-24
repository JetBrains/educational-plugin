package com.jetbrains.edu.cognifire.tutorial

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.EducationalCoreIcons.CourseView.Section
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.cognifire.messages.EduCognifireBundle
import com.jetbrains.edu.cognifire.tutorial.ContentType.MAIN
import com.jetbrains.edu.cognifire.tutorial.ContentType.TEST
import com.jetbrains.edu.cognifire.tutorial.TaskType.EDU_TASK
import com.jetbrains.edu.cognifire.tutorial.TaskType.THEORY_TASK
import com.jetbrains.edu.cognifire.utils.isCognifireApplicable
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.hasSections
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.joinPaths
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

/**
 * TutorialGenerator class is used to generate a tutorial in a course.
 * Depending on the course structure, this can be a lesson or a section.
 * The tutorial includes lesson designed to assist users in learning the new syntax for programming in the native language.
 * The tutorial can be accessed by right-clicking on the project root directory (main folder) and navigating to New -> Tutorial.
 */
// TODO: change the icon
class TutorialGenerator : DumbAwareAction(EduCognifireBundle.message("item.tutorial.title"), "", Section) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    val itemDir = generateTutorial(course)
    ProjectView.getInstance(project).select(itemDir, itemDir, true)
  }

  override fun update(event: AnActionEvent) {
    val presentation = event.presentation
    presentation.isEnabledAndVisible = false
    val project = event.getData(CommonDataKeys.PROJECT) ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (!CCUtils.isCourseCreator(project)) return
    if (!isCognifireApplicable(course)) return
    val selectedFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
    if (selectedFiles.firstOrNull() != project.courseDir) return
    if (CommonDataKeys.PSI_FILE.getData(event.dataContext) != null) return
    presentation.isEnabledAndVisible = true
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private fun generateTutorial(course: Course): VirtualFile {
    val container = if (course.hasSections) course.createSection() else course
    val lesson = container.createLesson(tutorialLesson.name) { lesson ->
      tutorialLesson.tasks.mapIndexed { index, tutorialTask ->
        createTask(tutorialTask.name, lesson, index + 1, tutorialTask.type) { task ->
          mutableListOf(createTaskFile(task, MAIN)).apply {
            if (tutorialTask.type == EDU_TASK) {
              add(createTaskFile(task, TEST))
            }
          }
        }
      }
    }
    val project = course.project ?: error("Project is not found")
    val virtualFile = if (course.hasSections && container is Section) {
      GeneratorUtils.createSection(project, container, project.courseDir)
    } else {
      GeneratorUtils.createLesson(project, lesson, project.courseDir)
    }
    saveStructureToYaml(lesson, course, container as? Section)
    return virtualFile
  }

  private fun saveStructureToYaml(lesson: Lesson, course: Course, section: Section? = null) {
    lesson.taskList.forEach { task -> YamlFormatSynchronizer.saveItemWithRemoteInfo(task) }
    YamlFormatSynchronizer.saveItem(lesson)
    if (section != null) {
      YamlFormatSynchronizer.saveItem(section)
    }
    YamlFormatSynchronizer.saveItem(course)
  }

  private fun Course.createSection() =
    Section().apply {
      val course = this@createSection
      name = EduCognifireBundle.message("item.tutorial.title")
      index = 0
      parent = course
      course.addItem(0, this)
    }

  private fun LessonContainer.createLesson(lessonName: String, createTasks: (Lesson) -> List<Task>) =
    FrameworkLesson().apply {
      val container = this@createLesson
      name = lessonName
      customPresentableName = lessonName.getPresentableName()
      index = 0
      parent = container
      isTemplateBased = false
      container.addLesson(this)
      createTasks(this).forEach { task -> addTask(task) }
    }

  private fun createTask(
    taskName: String,
    lesson: ItemContainer,
    taskIndex: Int,
    itemType: TaskType,
    createTaskFiles: (Task) -> List<TaskFile>
  ) =
    when (itemType) {
      THEORY_TASK -> TheoryTask()
      EDU_TASK -> EduTask()
    }.apply {
      name = taskName
      index = taskIndex
      customPresentableName = getTaskPresentableName(lesson, taskName)
      parent = lesson
      descriptionText =
        getFileContentFromResources(lesson.course, joinPaths(RESOURCE_FOLDER, lesson.name, taskName, DescriptionFormat.MD.fileName))
      descriptionFormat = DescriptionFormat.MD
      taskFiles = createTaskFiles(this).associateBy { it.name }
    }

  private fun getTaskPresentableName(lesson: ItemContainer, taskName: String) =
    "${lesson.presentableName} - ${taskName.getPresentableName()}"

  private fun createTaskFile(task: Task, contentType: ContentType): TaskFile {
    val fileName = getFileName(task.course, contentType)
    return TaskFile().apply {
      name = getPath(task, fileName, contentType)
      contents = InMemoryTextualContents(
        getFileContentFromResources(
          task.course,
          joinPaths(RESOURCE_FOLDER, task.lesson.name, task.name, fileName)
        )
      )
      isVisible = contentType.isVisible
      this.task = task
    }
  }

  private fun getFileName(course: Course, contentType: ContentType) = when (contentType) {
    TEST -> course.configurator?.courseBuilder?.testTemplateName(course) ?: error("Test file name not configured")
    MAIN -> course.configurator?.courseBuilder?.mainTemplateName(course) ?: error("Main file name not configured")
  }

  private fun getPath(task: Task, fileName: String, contentType: ContentType) = when (contentType) {
    TEST -> joinPaths(task.course.configurator?.testDirs?.firstOrNull(), fileName)
    MAIN -> joinPaths(
      task.course.configurator?.sourceDir ?: error("Can't find source dir for task ${task.name}"),
      PACKAGE,
      fileName
    )
  }

  private fun getFileContentFromResources(course: Course, path: String) = ApplicationManager.getApplication().runReadAction<String> {
    course.configurator?.javaClass?.getResource(path)
      ?.let { resourceUrl -> VfsUtil.findFileByURL(resourceUrl) }
      ?.let { virtualFile -> VfsUtil.loadText(virtualFile) }
  } ?: error("File from resources not found or unable to read: $path")

  private fun String.getPresentableName() = replace(capitalLetterRegex, " $1").trim()

  companion object {
    private const val RESOURCE_FOLDER = "/cognifire"
    private const val PACKAGE = "jetbrains/course/tutorial"
    private val capitalLetterRegex = Regex("([A-Z])")
    val tutorialLesson = TutorialLesson(
      "Tutorial", listOf(
        TutorialTask("Introduction", THEORY_TASK),
        TutorialTask("ProjectDescription", THEORY_TASK),
        TutorialTask("Function", EDU_TASK),
        TutorialTask("PromptBlock", EDU_TASK),
        TutorialTask("ReceivingUserInput", EDU_TASK),
        TutorialTask("FunctionCall", EDU_TASK),
        TutorialTask("IfExpression", EDU_TASK),
        TutorialTask("Run", EDU_TASK),
        TutorialTask("Correction", EDU_TASK),
        TutorialTask("Verification", THEORY_TASK),
        )
    )
  }
}
