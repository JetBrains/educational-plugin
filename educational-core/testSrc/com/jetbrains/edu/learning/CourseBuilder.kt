package com.jetbrains.edu.learning

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import java.util.regex.Pattern

private val OPENING_TAG: Pattern = Pattern.compile("<p>")
private val CLOSING_TAG: Pattern = Pattern.compile("</p>")

fun course(
  name: String = "Test Course",
  language: Language = PlainTextLanguage.INSTANCE,
  courseMode: String = EduNames.STUDY,
  buildCourse: CourseBuilder.() -> Unit
): Course {
  val builder = CourseBuilder()
  builder.withName(name)
  builder.withMode(courseMode)
  builder.buildCourse()
  val course = builder.course
  course.language = language.id
  return course
}

fun Course.createCourseFiles(project: Project, baseDir: VirtualFile, settings: Any) {
  @Suppress("UNCHECKED_CAST")
  val configurator = configurator as? EduConfigurator<Any>

  configurator
    ?.courseBuilder
    ?.getCourseProjectGenerator(this)
    ?.createCourseStructure(project, baseDir, settings)
}

class CourseBuilder {
  val course: Course = Course()

  fun withName(name: String) {
    course.name = name
  }

  fun withMode(courseMode: String) {
    course.courseMode = courseMode
  }

  fun lesson(name: String? = null, isFramework: Boolean = false, buildLesson: LessonBuilder.() -> Unit) {
    val lessonBuilder = LessonBuilder(course, null, if (isFramework) FrameworkLesson() else Lesson())
    val lesson = lessonBuilder.lesson
    lesson.index = course.lessons.size + 1
    val nextLessonIndex = course.lessons.size + 1
    lessonBuilder.withName(name ?: EduNames.LESSON + nextLessonIndex)
    course.addLesson(lesson)
    lessonBuilder.buildLesson()
  }

  fun section(name: String? = null, buildSection: SectionBuilder.() -> Unit) {
    val sectionBuilder = SectionBuilder(course, Section())
    val section = sectionBuilder.section
    section.index = course.lessons.size + 1
    val nextSectionIndex = course.items.size + 1
    sectionBuilder.withName(name ?: EduNames.SECTION + nextSectionIndex)
    course.addSection(section)
    sectionBuilder.buildSection()
  }
}
class SectionBuilder(val course: Course, val section: Section = Section()) {
  fun withName(name: String) {
    section.name = name
  }

  fun lesson(name: String? = null, buildLesson: LessonBuilder.() -> Unit) {
    val lessonBuilder = LessonBuilder(course, section, Lesson())
    lessonBuilder.lesson.index = section.lessons.size + 1
    val nextIndex = section.lessons.size + 1
    lessonBuilder.withName(name?: EduNames.LESSON + nextIndex)
    lessonBuilder.buildLesson()
    section.addLesson(lessonBuilder.lesson)
  }
}

class LessonBuilder(val course: Course, section: Section?, val lesson: Lesson = Lesson()) {

  init {
    lesson.course = course
    lesson.section = section
  }

  fun withName(name: String) {
    lesson.name = name
  }

  fun task(task: Task, name: String? = null, taskDescription: String? = null, buildTask: TaskBuilder.() -> Unit) {
    val taskBuilder = TaskBuilder(lesson, task)
    taskBuilder.task.index = lesson.taskList.size + 1
    val nextTaskIndex = lesson.taskList.size + 1
    taskBuilder.withName(name?: EduNames.TASK + nextTaskIndex)
    taskBuilder.withTaskDescription(taskDescription?: "solve task")
    taskBuilder.buildTask()
    lesson.addTask(taskBuilder.task)
  }

  fun eduTask(name: String? = null, taskDescription: String? = null, buildTask: TaskBuilder.() -> Unit) = task(EduTask(), name, taskDescription, buildTask)

  fun theoryTask(name: String? = null, taskDescription: String? = null, buildTask: TaskBuilder.() -> Unit) = task(TheoryTask(), name, taskDescription, buildTask)
}

class TaskBuilder(val lesson: Lesson, val task: Task) {
  init {
    task.lesson = lesson
  }
  fun withName(name: String) {
    task.name = name
  }

  fun withTaskDescription(text: String) {
    task.addTaskText(EduNames.TASK, text)
  }

  fun taskFile(name: String, text: String = "", buildTaskFile: (TaskFileBuilder.() -> Unit)? = null) {
    val taskFileBuilder = TaskFileBuilder(task)
    taskFileBuilder.withName(name)
    val textBuilder = StringBuilder(text.trimIndent())
    val placeholders = extractPlaceholdersFromText(textBuilder)
    taskFileBuilder.withText(textBuilder.toString())
    taskFileBuilder.withPlaceholders(placeholders)
    if (buildTaskFile != null) {
      taskFileBuilder.buildTaskFile()
    }
    val taskFile = taskFileBuilder.taskFile
    taskFile.task = task
    task.addTaskFile(taskFile)
  }

  private fun extractPlaceholdersFromText(text: StringBuilder): List<AnswerPlaceholder> {
    val openingMatcher = OPENING_TAG.matcher(text)
    val closingMatcher = CLOSING_TAG.matcher(text)
    val placeholders = mutableListOf<AnswerPlaceholder>()
    var pos = 0
    while (openingMatcher.find(pos)) {
      val answerPlaceholder = AnswerPlaceholder()
      val answerPlaceholderSubtaskInfo = AnswerPlaceholderSubtaskInfo()
      answerPlaceholder.subtaskInfos[0] = answerPlaceholderSubtaskInfo
      answerPlaceholder.offset = openingMatcher.start()
      if (!closingMatcher.find(openingMatcher.end())) {
        error("No matching closing tag found")
      }
      answerPlaceholder.length = closingMatcher.start() - openingMatcher.end()
      placeholders.add(answerPlaceholder)
      text.delete(closingMatcher.start(), closingMatcher.end())
      text.delete(openingMatcher.start(), openingMatcher.end())
      pos = answerPlaceholder.offset + answerPlaceholder.realLength
    }
    return placeholders
  }
}

class TaskFileBuilder(val task: Task) {
  val taskFile = TaskFile()

  init {
    taskFile.task = task
  }
  fun withName(name: String) {
    taskFile.name = name
  }

  fun withText(text: String) {
    taskFile.text = text
  }

  fun withPlaceholders(placeholders: List<AnswerPlaceholder>) {
    for (placeholder in placeholders) {
      placeholder.taskFile = taskFile
      taskFile.addAnswerPlaceholder(placeholder)
    }
  }

  fun placeholder(index: Int, taskText: String = "type here", dependency: String = "") {
    val answerPlaceholder = taskFile.answerPlaceholders[index]
    answerPlaceholder.taskText = taskText
    val createdDependency = AnswerPlaceholderDependency.create(answerPlaceholder, dependency)
    if (createdDependency != null) {
      answerPlaceholder.placeholderDependency = createdDependency
    }
  }
}
