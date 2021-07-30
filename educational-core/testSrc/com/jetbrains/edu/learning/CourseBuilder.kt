package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import org.intellij.lang.annotations.Language
import java.io.File
import java.util.*
import java.util.regex.Pattern

private val OPENING_TAG: Pattern = Pattern.compile("<p>")
private val CLOSING_TAG: Pattern = Pattern.compile("</p>")

fun course(
  name: String = "Test Course",
  language: com.intellij.lang.Language = PlainTextLanguage.INSTANCE,
  description: String = "Test Course Description",
  environment: String = "",
  courseMode: String = EduNames.STUDY,
  courseProducer: () -> Course = ::EduCourse,
  buildCourse: CourseBuilder.() -> Unit
): Course {
  val builder = CourseBuilder(courseProducer())
  builder.withName(name)
  builder.withMode(courseMode)
  val course = builder.course
  course.language = language.id
  course.environment = environment
  course.description = description
  builder.buildCourse()
  return course
}

fun Course.createCourseFiles(
  project: Project,
  module: Module,
  baseDir: VirtualFile = LightPlatformTestCase.getSourceRoot(),
  settings: Any = Unit
) {
  @Suppress("UNCHECKED_CAST")
  val configurator = configurator as? EduConfigurator<Any>

  configurator
    ?.courseBuilder
    ?.getCourseProjectGenerator(this)
    ?.createCourseStructure(project, module, baseDir, settings)
}

abstract class LessonOwnerBuilder(val course: Course) {

  protected abstract val nextLessonIndex: Int
  protected abstract fun addLesson(lesson: Lesson)

  fun frameworkLesson(name: String? = null, isTemplateBased: Boolean = true, buildLesson: LessonBuilder<FrameworkLesson>.() -> Unit = {}) {
    val lesson = FrameworkLesson().also { it.isTemplateBased = isTemplateBased }
    lesson(name, lesson, buildLesson)
  }

  fun lesson(name: String? = null, buildLesson: LessonBuilder<Lesson>.() -> Unit = {}) {
    lesson(name, Lesson(), buildLesson)
  }

  fun station(name: String? = null, buildLesson: LessonBuilder<CheckiOStation>.() -> Unit = {}) {
    lesson(name, CheckiOStation(), buildLesson)
  }

  protected fun <T : Lesson> lesson(name: String? = null, lesson: T, buildLesson: LessonBuilder<T>.() -> Unit) {
    val lessonBuilder = LessonBuilder(course, null, lesson)
    lesson.index = nextLessonIndex
    lessonBuilder.withName(name ?: EduNames.LESSON + nextLessonIndex)
    addLesson(lesson)
    lessonBuilder.buildLesson()
  }

  fun additionalFiles(buildTaskFiles: AdditionalFilesBuilder.() -> Unit) {
    val builder = AdditionalFilesBuilder(course)
    builder.buildTaskFiles()
  }
}

class CourseBuilder(course: Course) : LessonOwnerBuilder(course) {

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
    section.index = course.items.size + 1
    val nextSectionIndex = course.items.size + 1
    sectionBuilder.withName(name ?: EduNames.SECTION + nextSectionIndex)
    course.addSection(section)
    sectionBuilder.buildSection()
  }

  fun additionalFile(name: String, text: String = "", buildTaskFile: TaskFileBuilder.() -> Unit = {}) {
    val builder = TaskFileBuilder()
    builder.withName(name)
    builder.withText(text)
    builder.buildTaskFile()
    course.additionalFiles.add(builder.taskFile)
  }
}

class SectionBuilder(course: Course, val section: Section = Section()) : LessonOwnerBuilder(course) {
  init {
    section.course = course
  }

  override val nextLessonIndex: Int get() = section.lessons.size + 1

  override fun addLesson(lesson: Lesson) {
    section.addLesson(lesson)
  }

  fun withName(name: String) {
    section.name = name
  }
}

class LessonBuilder<T : Lesson>(val course: Course, section: Section?, val lesson: T) {

  init {
    lesson.course = course
    lesson.section = section
  }

  fun withName(name: String) {
    lesson.name = name
  }

  private fun task(
    task: Task,
    name: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    // we want to know task files order in tests
    task.taskFiles = LinkedHashMap()
    val taskBuilder = TaskBuilder(lesson, task)
    taskBuilder.task.index = lesson.taskList.size + 1
    val nextTaskIndex = lesson.taskList.size + 1
    taskBuilder.withName(name ?: EduNames.TASK + nextTaskIndex)
    taskBuilder.withTaskDescription(taskDescription ?: "solve task", taskDescriptionFormat)
    taskBuilder.withStepId(stepId)
    taskBuilder.withUpdateDate(updateDate)
    taskBuilder.buildTask()
    for ((_, taskFile) in taskBuilder.task.taskFiles) {
      taskFile.isVisible = taskBuilder.explicitVisibility[taskFile.name] ?: !EduUtils.isTestsFile(taskBuilder.task, taskFile.name)
    }

    lesson.addTask(taskBuilder.task)
  }

  fun eduTask(
    name: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(EduTask(), name, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)

  fun theoryTask(
    name: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(TheoryTask(), name, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)

  fun outputTask(
    name: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(OutputTask(), name, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)

  fun choiceTask(
    name: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    choiceOptions: Map<String, ChoiceOptionStatus>,
    isMultipleChoice: Boolean = false,
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val choiceTask = ChoiceTask()
    task(choiceTask, name, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)
    choiceTask.choiceOptions = choiceOptions.map { ChoiceOption(it.key, it.value) }
    choiceTask.isMultipleChoice = isMultipleChoice
  }

  fun mission(
    name: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    code: String = "",
    secondsFromChange: Long = 0,
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val mission = CheckiOMission()
    task(mission, name, taskDescription, taskDescriptionFormat, buildTask = buildTask)
    mission.code = code
    mission.secondsFromLastChangeOnServer = secondsFromChange
  }

  fun videoTask(
    name: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    thumbnail: String = "",
    sources: Map<String, String>,
    currentTime: Int = 0,
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val videoTask = VideoTask()
    task(videoTask, name, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)
    videoTask.currentTime = currentTime
    videoTask.thumbnail = thumbnail
    videoTask.sources = sources.map { VideoSource(it.key, it.value) }
  }

  fun codeTask(
    name: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(CodeTask(), name, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)

  fun codeforcesTask(
    name: String? = null,
    taskDescription: String? = null,
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val codeforcesTask = CodeforcesTask()
    task(codeforcesTask, name, taskDescription, DescriptionFormat.HTML, buildTask = buildTask)
  }

  fun ideTask(
    name: String? = null,
    taskDescription: String? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(IdeTask(), name, taskDescription, DescriptionFormat.HTML, stepId, updateDate, buildTask)
}

class TaskBuilder(val lesson: Lesson, val task: Task) {

  private val _explicitVisibility: MutableMap<String, Boolean> = mutableMapOf()

  val explicitVisibility: Map<String, Boolean> get() = _explicitVisibility

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

  fun withUpdateDate(date: Date) {
    task.updateDate = date
  }

  fun withStepId(stepId: Int) {
    task.id = stepId
  }

  /**
   * Creates task file with given [name] and [text].
   *
   * You can also create placeholders for this task file using `<p>` tag.
   *
   * For example, for `fun foo() = <p>TODO()</p>` text
   * it creates task file with `fun foo() = TODO()` text and placeholder with `TODO()` as placeholder text.
   */
  fun taskFile(
    name: String, text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) {
    val taskFileBuilder = TaskFileBuilder(task)
    taskFileBuilder.withName(name)
    val textBuilder = StringBuilder(text.trimIndent())
    val placeholders = extractPlaceholdersFromText(textBuilder)
    taskFileBuilder.withText(textBuilder.toString())
    taskFileBuilder.withPlaceholders(placeholders)
    taskFileBuilder.buildTaskFile()
    val taskFile = taskFileBuilder.taskFile
    if (visible != null) {
      _explicitVisibility[name] = visible
    }
    taskFile.task = task
    task.addTaskFile(taskFile)
  }

  fun checkResultFile(status: CheckStatus, message: String = "") {
    taskFile(PlainTextConfigurator.CHECK_RESULT_FILE, "$status $message")
  }

  fun kotlinTaskFile(
    name: String,
    @Language("kotlin") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile)

  fun javaTaskFile(
    name: String,
    @Language("JAVA") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile)

  fun pythonTaskFile(
    name: String,
    @Language("Python") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile)

  fun scalaTaskFile(
    name: String,
    @Language("Scala") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile)

  fun rustTaskFile(
    name: String,
    @Language("Rust") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile)

  fun goTaskFile(
    name: String,
    @Language("Go") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile)

  fun cppTaskFile(
    name: String,
    @Language("ObjectiveC") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile)

  fun xmlTaskFile(
    name: String,
    @Language("XML") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile)

  fun taskFileFromResources(
    disposable: Disposable,
    name: String,
    path: String,
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) {
    val ioFile = File(path)
    VfsRootAccess.allowRootAccess(disposable, ioFile.absolutePath)
    val file = LocalFileSystem.getInstance().findFileByIoFile(ioFile) ?: error("Can't find `$path`")
    val text = file.loadEncodedContent()
    taskFile(name, text, visible, buildTaskFile)
  }

  fun dir(dirName: String, buildTask: TaskBuilder.() -> Unit) {
    val tmpTask = EduTask()
    val innerBuilder = TaskBuilder(lesson, tmpTask)
    innerBuilder.buildTask()
    for ((_, taskFile) in tmpTask.taskFiles) {
      val visibility = innerBuilder.explicitVisibility[taskFile.name]
      taskFile.name = "$dirName/${taskFile.name}"
      task.addTaskFile(taskFile)
      if (visibility != null) {
        _explicitVisibility[taskFile.name] = visibility
      }
    }
  }

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
      pos = answerPlaceholder.endOffset
    }
    return placeholders
  }
}

class AdditionalFilesBuilder(val course: Course) {

  fun taskFile(
    name: String, text: String = "",
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) {
    val taskFileBuilder = TaskFileBuilder()
    taskFileBuilder.withName(name)
    val textBuilder = StringBuilder(text.trimIndent())
    taskFileBuilder.withText(textBuilder.toString())
    taskFileBuilder.buildTaskFile()
    val taskFile = taskFileBuilder.taskFile
    course.additionalFiles.add(taskFile)
  }
}

class TaskFileBuilder(val task: Task? = null) {
  val taskFile = TaskFile()

  init {
    taskFile.task = task
  }
  fun withName(name: String) {
    taskFile.name = name
  }

  fun withText(text: String) {
    taskFile.setText(text)
  }

  fun withPlaceholders(placeholders: List<AnswerPlaceholder>) {
    for (placeholder in placeholders) {
      placeholder.taskFile = taskFile
      taskFile.addAnswerPlaceholder(placeholder)
    }
  }

  fun placeholder(index: Int, possibleAnswer: String? = null, placeholderText: String? = null) {
    val answerPlaceholder = taskFile.answerPlaceholders[index]
    if (possibleAnswer != null) {
      answerPlaceholder.possibleAnswer = possibleAnswer
    }
    if (placeholderText != null) {
      answerPlaceholder.placeholderText = placeholderText
    }
  }
}
