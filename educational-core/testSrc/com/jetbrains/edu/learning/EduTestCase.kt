package com.jetbrains.edu.learning

import com.intellij.lang.Language
import com.intellij.openapi.components.impl.ComponentManagerImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.impl.FileEditorProviderManagerImpl
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.intellij.ui.docking.DockContainer
import com.intellij.ui.docking.DockManager
import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.io.IOException

abstract class EduTestCase : LightPlatformCodeInsightFixtureTestCase() {
  private lateinit var myManager: FileEditorManagerImpl
  private lateinit var myOldManager: FileEditorManager
  private lateinit var myOldDockContainers: Set<DockContainer>

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    registerPlainTextConfigurator(myFixture.testRootDisposable)
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
    lesson.name = "lesson$index"
    (1..taskCount)
        .map { createTask(index, it) }
        .forEach { lesson.addTask(it) }
    lesson.index = index
    return lesson
  }

  @Throws(IOException::class) private fun createTask(lessonIndex: Int, taskIndex: Int): Task {
    val task = EduTask()
    task.name = "task$taskIndex"
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

  fun courseWithFiles(
    name: String = "Test Course",
    courseMode: String = EduNames.STUDY,
    language: Language = PlainTextLanguage.INSTANCE,
    settings: Any = Unit,
    buildCourse: CourseBuilder.() -> Unit
  ): Course {
    return course(name, language, courseMode, buildCourse).apply {
      createCourseFiles(project, LightPlatformTestCase.getSourceRoot(), settings)
    }
  }

  protected fun getCourse(): Course = StudyTaskManager.getInstance(project).course!!

  protected fun findPlaceholder(lessonIndex: Int, taskIndex: Int, taskFile: String, placeholderIndex: Int) : AnswerPlaceholder {
    return getCourse().lessons[lessonIndex].taskList[taskIndex].taskFiles[taskFile]!!.answerPlaceholders[placeholderIndex]
  }

  protected fun findTask(lessonIndex: Int, taskIndex: Int): Task = getCourse().lessons[lessonIndex].taskList[taskIndex]

  protected fun findVirtualFile(lessonIndex: Int, taskIndex: Int, taskFilePath: String): VirtualFile {
    val task = findTask(lessonIndex, taskIndex)
    val taskDir = task.getTaskDir(project)!!
    val taskFile = task.getTaskFile(taskFilePath)!!
    return EduUtils.findTaskFileInDir(taskFile, taskDir)!!
  }
}
