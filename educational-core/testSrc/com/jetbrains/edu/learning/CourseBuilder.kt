package com.jetbrains.edu.learning

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import org.intellij.lang.annotations.Language
import java.util.regex.Pattern

private val OPENING_TAG: Pattern = Pattern.compile("<p>")
private val CLOSING_TAG: Pattern = Pattern.compile("</p>")

fun course(
  name: String = "Test Course",
  language: com.intellij.lang.Language = PlainTextLanguage.INSTANCE,
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

abstract class LessonOwnerBuilder(val course: Course) {

  protected abstract val nextLessonIndex: Int
  protected abstract fun addLesson(lesson: Lesson)

  fun frameworkLesson(name: String? = null, buildLesson: LessonBuilder.() -> Unit = {}) {
    lesson(name, true, buildLesson)
  }

  fun lesson(name: String? = null, buildLesson: LessonBuilder.() -> Unit = {}) {
    lesson(name, false, buildLesson)
  }

  protected fun lesson(name: String? = null, isFramework: Boolean = false, buildLesson: LessonBuilder.() -> Unit) {
    val lessonBuilder = LessonBuilder(course, null, if (isFramework) FrameworkLesson() else Lesson())
    val lesson = lessonBuilder.lesson
    lesson.index = nextLessonIndex
    lessonBuilder.withName(name ?: EduNames.LESSON + nextLessonIndex)
    addLesson(lesson)
    lessonBuilder.buildLesson()
  }
}

class CourseBuilder : LessonOwnerBuilder(Course()) {

  override val nextLessonIndex: Int get() = course.lessons.size + 1

  fun withName(name: String) {
    course.name = name
  }

  fun withMode(courseMode: String) {
    course.courseMode = courseMode
  }

  override fun addLesson(lesson: Lesson) {
    course.addLesson(lesson)
  }

  fun section(name: String? = null, buildSection: SectionBuilder.() -> Unit = {}) {
    val sectionBuilder = SectionBuilder(course, Section())
    val section = sectionBuilder.section
    section.index = course.lessons.size + 1
    val nextSectionIndex = course.items.size + 1
    sectionBuilder.withName(name ?: EduNames.SECTION + nextSectionIndex)
    course.addSection(section)
    sectionBuilder.buildSection()
  }
}

class SectionBuilder(course: Course, val section: Section = Section()) : LessonOwnerBuilder(course) {

  override val nextLessonIndex: Int get() = section.lessons.size + 1

  override fun addLesson(lesson: Lesson) {
    section.addLesson(lesson)
  }

  fun withName(name: String) {
    section.name = name
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

  fun task(
    task: Task,
    name: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val taskBuilder = TaskBuilder(lesson, task)
    taskBuilder.task.index = lesson.taskList.size + 1
    val nextTaskIndex = lesson.taskList.size + 1
    taskBuilder.withName(name?: EduNames.TASK + nextTaskIndex)
    taskBuilder.withTaskDescription(taskDescription ?: "solve task", taskDescriptionFormat)
    taskBuilder.buildTask()
    lesson.addTask(taskBuilder.task)
  }

  fun eduTask(
    name: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(EduTask(), name, taskDescription, taskDescriptionFormat, buildTask)

  fun theoryTask(
    name: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(TheoryTask(), name, taskDescription, taskDescriptionFormat, buildTask)

  fun outputTask(
    name: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(OutputTask(), name, taskDescription, taskDescriptionFormat, buildTask)
}

class TaskBuilder(val lesson: Lesson, val task: Task) {
  init {
    task.lesson = lesson
  }
  fun withName(name: String) {
    task.name = name
  }

  fun withTaskDescription(text: String, format: DescriptionFormat? = null) {
    task.descriptionText = text
    task.descriptionFormat = format ?: DescriptionFormat.HTML
  }

  /**
   * Creates task file with given [name] and [text].
   *
   * You can also create placeholders for this task file using `<p>` tag.
   *
   * For example, for `fun foo() = <p>TODO()</p>` text
   * it creates task file with `fun foo() = TODO()` text and placeholder with `TODO()` as placeholder text.
   */
  fun taskFile(name: String, text: String = "", buildTaskFile: TaskFileBuilder.() -> Unit = {}) {
    val taskFileBuilder = TaskFileBuilder(task)
    taskFileBuilder.withName(name)
    val textBuilder = StringBuilder(text.trimIndent())
    val placeholders = extractPlaceholdersFromText(textBuilder)
    taskFileBuilder.withText(textBuilder.toString())
    taskFileBuilder.withPlaceholders(placeholders)
    taskFileBuilder.buildTaskFile()
    val taskFile = taskFileBuilder.taskFile
    taskFile.task = task
    task.addTaskFile(taskFile)
  }

  fun kotlinTaskFile(
    name: String,
    @Language("kotlin") text: String = "",
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, buildTaskFile)

  fun javaTaskFile(
    name: String,
    @Language("JAVA") text: String = "",
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, buildTaskFile)

  fun pythonTaskFile(
    name: String,
    @Language("Python") text: String = "",
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, buildTaskFile)

  fun scalaTaskFile(
    name: String,
    @Language("Scala") text: String = "",
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, buildTaskFile)

  fun testFile(name: String, text: String = "") {
    task.addTestsTexts(name, text)
  }

  fun kotlinTestFile(name: String, @Language("kotlin") text: String = "") = testFile(name, text)
  fun javaTestFile(name: String, @Language("JAVA") text: String = "") = testFile(name, text)
  fun pythonTestFile(name: String, @Language("Python") text: String = "") = testFile(name, text)
  fun scalaTestFile(name: String, @Language("Scala") text: String = "") = testFile(name, text)

  private fun extractPlaceholdersFromText(text: StringBuilder): List<AnswerPlaceholder> {
    val openingMatcher = OPENING_TAG.matcher(text)
    val closingMatcher = CLOSING_TAG.matcher(text)
    val placeholders = mutableListOf<AnswerPlaceholder>()
    var pos = 0
    while (openingMatcher.find(pos)) {
      val answerPlaceholder = AnswerPlaceholder()
      if (!closingMatcher.find(openingMatcher.end())) {
        error("No matching closing tag found")
      }
      answerPlaceholder.offset = openingMatcher.start()
      answerPlaceholder.length = closingMatcher.start() - openingMatcher.end()
      answerPlaceholder.placeholderText = text.substring(openingMatcher.end(), closingMatcher.start())
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

  fun placeholder(index: Int, possibleAnswer: String = "", dependency: String = "") {
    val answerPlaceholder = taskFile.answerPlaceholders[index]
    answerPlaceholder.possibleAnswer = possibleAnswer
    val createdDependency = AnswerPlaceholderDependency.create(answerPlaceholder, dependency)
    if (createdDependency != null) {
      answerPlaceholder.placeholderDependency = createdDependency
    }
  }
}
