package com.jetbrains.edu.learning

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.components.impl.ComponentManagerImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.impl.FileEditorProviderManagerImpl
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.intellij.ui.docking.DockContainer
import com.intellij.ui.docking.DockManager
import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import java.io.IOException
import java.util.regex.Pattern

abstract class EduTestCase : LightPlatformCodeInsightFixtureTestCase() {
  private lateinit var myManager: FileEditorManagerImpl
  private lateinit var myOldManager: FileEditorManager
  private lateinit var myOldDockContainers: Set<DockContainer>

  companion object {
    private val OPENING_TAG: Pattern = Pattern.compile("<p>")
    private val CLOSING_TAG: Pattern = Pattern.compile("</p>")
  }


  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    registerPlainTextConfigurator()
    createCourse()

    val dockManager = DockManager.getInstance(myFixture.project)
    myOldDockContainers = dockManager.containers
    myManager = FileEditorManagerImpl(myFixture.project, dockManager)
    myOldManager = (myFixture.project as ComponentManagerImpl).registerComponentInstance<FileEditorManager>(FileEditorManager::class.java, myManager)
    (FileEditorProviderManager.getInstance() as FileEditorProviderManagerImpl).clearSelectedProviders()
  }

  override fun tearDown() {
    try {
      DockManager.getInstance(myFixture.project).containers
          .filterNot { myOldDockContainers.contains(it) }
          .forEach { Disposer.dispose(it) }

      (myFixture.project as ComponentManagerImpl).registerComponentInstance(FileEditorManager::class.java, myOldManager)
      myManager.closeAllFiles()

      EditorHistoryManager.getInstance(myFixture.project).files.forEach {
        EditorHistoryManager.getInstance(myFixture.project).removeFile(it)
      }

      (FileEditorProviderManager.getInstance() as FileEditorProviderManagerImpl).clearSelectedProviders()
    }
    finally {
      super.tearDown()
    }
  }

  @Throws(IOException::class)
  protected open fun createCourse() {
  }

  @Throws(IOException::class)
  protected fun createLesson(index: Int, taskCount: Int): Lesson {
    val lesson = Lesson()
    lesson.name = "lesson" + index
    (1..taskCount)
        .map { createTask(index, it) }
        .forEach { lesson.addTask(it) }
    lesson.index = index
    return lesson
  }

  @Throws(IOException::class) private fun createTask(lessonIndex: Int, taskIndex: Int): Task {
    val task = EduTask()
    task.name = "task" + taskIndex
    task.index = taskIndex
    createTaskFile(lessonIndex, task, "taskFile$taskIndex.txt")
    return task
  }

  @Throws(IOException::class)
  private fun createTaskFile(lessonIndex: Int, task: Task, taskFilePath: String) {
    val taskFile = TaskFile()
    taskFile.task = task
    task.getTaskFiles().put(taskFilePath, taskFile)
    taskFile.name = taskFilePath

    val fileName = "lesson" + lessonIndex + "/" + task.name + "/" + taskFilePath
    val file = myFixture.findFileInTempDir(fileName)
    taskFile.text = VfsUtilCore.loadText(file)

    FileEditorManager.getInstance(myFixture.project).openFile(file, true)
    val document = FileDocumentManager.getInstance().getDocument(file)
    for (placeholder in CCTestCase.getPlaceholders(document, true)) {
      taskFile.addAnswerPlaceholder(placeholder)
    }
    taskFile.sortAnswerPlaceholders()
  }

  protected fun configureByTaskFile(lessonIndex: Int, taskIndex: Int, taskFileName: String) {
    val fileName = "lesson$lessonIndex/task$taskIndex/$taskFileName"
    val file = myFixture.findFileInTempDir(fileName)
    myFixture.configureFromExistingVirtualFile(file)
    FileEditorManager.getInstance(myFixture.project).openFile(file, true)
  }

  override fun getTestDataPath(): String {
    return "testData"
  }

  private fun registerPlainTextConfigurator() {
    val extension = LanguageExtensionPoint<Annotator>()
    extension.language = PlainTextLanguage.INSTANCE.id
    extension.implementationClass = PlainTextConfigurator::class.java.name
    PlatformTestUtil.registerExtension(
      ExtensionPointName.create(EduConfigurator.EP_NAME), extension, myFixture.project)
  }

  class PlainTextConfigurator : EduConfigurator<Unit> {
    override fun getCourseBuilder() = object : EduCourseBuilder<Unit> {
      override fun createTaskContent(project: Project, task: Task, parentDirectory: VirtualFile, course: Course): VirtualFile? = null
      override fun getLanguageSettings(): EduCourseBuilder.LanguageSettings<Unit> = EduCourseBuilder.LanguageSettings { Unit }
    }

    override fun getTestFileName() = "test.txt"

    override fun excludeFromArchive(name: String) = false

    override fun getTaskCheckerProvider() = TaskCheckerProvider{ task, project -> object: TaskChecker<EduTask>(task, project) {
      override fun check(): CheckResult {
        return CheckResult(CheckStatus.Solved, "")
      }
    }}
  }


  fun course(name: String = "Test Course", language: Language = PlainTextLanguage.INSTANCE, buildCourse: CourseBuilder.() -> Unit): Course {
    val builder = CourseBuilder()
    builder.withName(name)
    builder.buildCourse()
    val course = builder.course
    course.language = language.id
    StudyTaskManager.getInstance(myFixture.project).course = course
    createCourseFiles(course)
    return course
  }

  private fun createCourseFiles(course: Course) {
    for (lesson in course.lessons) {
      val lessonDirName = EduNames.LESSON + lesson.index
      for (task in lesson.taskList) {
        val taskDirName = EduNames.TASK + task.index
        val sourceDir = if (course.sourceDir.isNullOrEmpty()) "" else "${course.sourceDir}/"
        for (taskFile in task.getTaskFiles().values) {
          val path = "${myFixture.project.baseDir.path}/$lessonDirName/$taskDirName/$sourceDir${taskFile.name}"
          myFixture.tempDirFixture.createFile(path, taskFile.text)
        }
      }
    }
  }

  class CourseBuilder {
    val course: Course = Course()

    fun withName(name: String) {
      course.name = name
    }

    fun lesson(name: String? = null, buildLesson: LessonBuilder.() -> Unit) {
      val lessonBuilder = LessonBuilder(course)
      val nextLessonIndex = course.lessons.size + 1
      lessonBuilder.withName(name ?: EduNames.LESSON + nextLessonIndex)
      lessonBuilder.buildLesson()
      val lesson = lessonBuilder.lesson
      course.addLesson(lesson)
      lesson.index = course.lessons.size
    }
  }

  class LessonBuilder(val course: Course) {
    val lesson = Lesson()

    init {
      lesson.course = course
    }

    fun withName(name: String) {
      lesson.name = name
    }

    fun task(task: Task, name: String? = null, buildTask: TaskBuilder.() -> Unit) {
      val taskBuilder = TaskBuilder(lesson, task)
      val nextTaskIndex = lesson.taskList.size + 1
      taskBuilder.withName(name?: EduNames.TASK + nextTaskIndex)
      taskBuilder.buildTask()
      lesson.addTask(taskBuilder.task)
      taskBuilder.task.index = lesson.taskList.size
    }

    fun eduTask(name: String? = null, buildTask: TaskBuilder.() -> Unit) = task(EduTask(), name, buildTask)

    fun theoryTask(name: String? = null, text: String, buildTask: TaskBuilder.()-> Unit) = task(TheoryTask(), name, buildTask)
  }

  class TaskBuilder(val lesson: Lesson, val task: Task) {
    init {
      task.lesson = lesson
    }
    fun withName(name: String) {
      task.name = name
    }

    fun taskFile(name: String, text: String, buildTaskFile: (TaskFileBuilder.() -> Unit)? = null) {
      val taskFileBuilder = TaskFileBuilder(task)
      taskFileBuilder.withName(name)
      val textBuffer = StringBuilder(text)
      val placeholders = extractPlaceholdersFromText(textBuffer)
      taskFileBuilder.withText(textBuffer.toString())
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
          Logger.getInstance(EduTestCase::class.java).error("No matching closing tag found")
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

    fun placeholder(index: Int, taskText: String = "type here") {
      val answerPlaceholder = taskFile.answerPlaceholders[index]
      answerPlaceholder.taskText = taskText
    }
  }

  fun getCourse(): Course = StudyTaskManager.getInstance(project).course!!

}


