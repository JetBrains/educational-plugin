package com.jetbrains.edu.learning.courseGeneration.jarvis.tutorial

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_MD
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask.Companion.EDU_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask.Companion.THEORY_TASK_TYPE
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.joinPaths
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

/**
 * TutorialGenerator is a utility object for generating a tutorial section in a course.
 * The section includes lessons designed to assist users in learning the new syntax for programming in the native language.
 * The tutorial section can be accessed by navigating to New -> Section -> Tutorial.
 */
object TutorialGenerator {
  fun generateTutorial(course: Course): VirtualFile {
    val section = course.createSection()
    lessons.forEach { (lessonName, taskList) ->
      section.createLesson(lessonName) { lesson ->
        taskList.mapIndexed { index, (taskName, itemType) ->
          createTask(taskName, lesson, index + 1, itemType) { task ->
            mutableListOf(createTaskFile(task)).apply {
              if (itemType == EDU_TASK_TYPE) {
                add(createTaskFile(task, true))
              }
            }
          }
        }
      }
    }
    val project = course.project ?: error("Project is not found")
    val virtualFile = GeneratorUtils.createSection(project, section, project.courseDir)
    section.lessons.forEach { it.taskList.forEach { task -> YamlFormatSynchronizer.saveItemWithRemoteInfo(task) } }
    section.lessons.forEach { lesson -> YamlFormatSynchronizer.saveItem(lesson) }
    YamlFormatSynchronizer.saveItem(section)
    YamlFormatSynchronizer.saveItem(course)
    return virtualFile
  }

  private fun Course.createSection() =
    Section().apply {
      name = EduCoreBundle.message("item.tutorial.title")
      index = 0
      parent = this@createSection
      this@createSection.addItem(0, this)
    }

  private fun Section.createLesson(lessonName: String, createTasks: (Lesson) -> List<Task>) =
    FrameworkLesson().apply {
      name = lessonName
      customPresentableName = lessonName.getPresentableName()
      index = this@createLesson.items.size + 1
      parent = this@createLesson
      this@createLesson.addLesson(this)
      createTasks(this).forEach { task -> addTask(task) }
    }

  private fun createTask(taskName: String, lesson: ItemContainer, taskIndex: Int, itemType: String, createTaskFiles: (Task) -> List<TaskFile>) =
    when (itemType) {
      THEORY_TASK_TYPE -> TheoryTask()
      EDU_TASK_TYPE -> EduTask()
      else -> error("Not supported task type for tutorial lesson")
    }.apply {
      name = taskName
      index = taskIndex
      customPresentableName = "${lesson.parent.presentableName} - ${lesson.presentableName} - ${taskName.getPresentableName()}"
      parent = lesson
      descriptionText = getFileContentFromResources(lesson.course, joinPaths(RESOURCE_FOLDER, lesson.name, taskName, TASK_MD))
      descriptionFormat = DescriptionFormat.MD
      taskFiles = createTaskFiles(this).associateBy { it.name }
    }

  private fun createTaskFile(task: Task, isTests: Boolean = false): TaskFile {
    val fileName = getFileName(task.course, isTests)
    return TaskFile().apply {
      name = getPath(task, fileName, isTests)
      contents = InMemoryTextualContents(getFileContentFromResources(
        task.course,
        joinPaths(RESOURCE_FOLDER, task.lesson.name, task.name, fileName)
      ))
      isVisible = !isTests
      this.task = task
    }
  }

  private fun getFileName(course: Course, isTests: Boolean) = if (isTests) {
    course.configurator?.courseBuilder?.testTemplateName(course) ?: error("Test file name not configured")
  } else {
    course.configurator?.courseBuilder?.mainTemplateName(course) ?: error("Main file name not configured")
  }

  private fun getPath(task: Task, fileName: String, isTests: Boolean) = if (isTests) {
    joinPaths(task.course.configurator?.testDirs?.firstOrNull(), fileName)
  } else {
    joinPaths(
      task.course.configurator?.sourceDir ?: error("Can't find source dir for task ${task.name}"),
      PACKAGE + task.lesson.name.toPackageFormat(),
      fileName
    )
  }

  private fun getFileContentFromResources(course: Course, path: String) = course.configurator?.javaClass?.getResource(path)?.readText()
                                                          ?: error("File from resources not found: $path")

  private fun String.getPresentableName() = replace(capitalLetterRegex, " $1").trim()

  private fun String.toPackageFormat() = replace(capitalLetterRegex, "/$1").lowercase()

  private const val RESOURCE_FOLDER = "/jarvis/Tutorial"
  private const val PACKAGE = "jetbrains/course/tutorial"
  private val capitalLetterRegex = Regex("([A-Z])")
  val lessons = listOf(
    "Introduction" to listOf ("TutorialIntroduction" to THEORY_TASK_TYPE),
    "DogYears" to listOf (
      "Introduction" to THEORY_TASK_TYPE,
      "Function" to EDU_TASK_TYPE,
      "DescriptionBlock" to EDU_TASK_TYPE,
      "ReceivingUserInput" to EDU_TASK_TYPE,
      "VariableDeclaration" to EDU_TASK_TYPE,
      "FunctionCall" to EDU_TASK_TYPE,
      "IfExpression" to EDU_TASK_TYPE,
      "Run" to EDU_TASK_TYPE,
      "Correction" to EDU_TASK_TYPE,
      "AcceptanceOfCode" to EDU_TASK_TYPE
    )
  )
}
