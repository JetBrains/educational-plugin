package com.jetbrains.edu.learning

import com.google.common.collect.Lists
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.impl.FileEditorProviderManagerImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorPsiDataProvider
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.intellij.ui.docking.DockContainer
import com.intellij.ui.docking.DockManager
import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EducationalExtensionPoint
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.configurators.FakeGradleConfigurator
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.handlers.UserCreatedFileListener
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import java.io.IOException

abstract class EduTestCase : LightPlatformCodeInsightFixtureTestCase() {
  private lateinit var myManager: FileEditorManagerImpl
  private lateinit var myOldDockContainers: Set<DockContainer>

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    // In this method course is set before course files are created so `CCProjectComponent.createYamlConfigFilesIfMissing` is called
    // for course with no files. This flag is checked in this method and it does nothing if the flag is false
    project.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, false)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE, HYPERSKILL)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE,
                         CheckiONames.CHECKIO_TYPE)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE,
                         StepikNames.STEPIK_TYPE)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE,
                         environment = EduNames.ANDROID)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE,
                         CourseraNames.COURSE_TYPE)
    registerConfigurator(myFixture.testRootDisposable, FakeGradleConfigurator::class.java, FakeGradleBasedLanguage)
    registerConfigurator(myFixture.testRootDisposable, FakeGradleConfigurator::class.java, FakeGradleBasedLanguage, HYPERSKILL)
    registerAdditionalResourceBundleProviders(testRootDisposable)

    val dockManager = DockManager.getInstance(myFixture.project)
    myOldDockContainers = dockManager.containers
    myManager = createFileEditorManager(myFixture.project)
    // Copied from TestEditorManagerImpl's constructor
    myManager.registerExtraEditorDataProvider(TextEditorPsiDataProvider(), null)
    project.registerComponent(FileEditorManager::class.java, myManager, testRootDisposable)
    (FileEditorProviderManager.getInstance() as FileEditorProviderManagerImpl).clearSelectedProviders()
    CheckActionListener.reset()
    val connection = project.messageBus.connect(testRootDisposable)
    connection.subscribe(StudyTaskManager.COURSE_SET, object : CourseSetListener {
      override fun courseSet(course: Course) {
        EduDocumentListener.setGlobalListener(project, testRootDisposable)
        connection.disconnect()
      }
    })
    createCourse()
  }

  override fun tearDown() {
    try {
      DockManager.getInstance(myFixture.project).containers
          .filterNot { myOldDockContainers.contains(it) }
          .forEach { Disposer.dispose(it) }

//      project.registerComponent(FileEditorManager::class.java, myOldManager)
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
    task.taskFiles[taskFilePath] = taskFile
    taskFile.name = taskFilePath

    val fileName = "lesson" + lessonIndex + "/" + task.name + "/" + taskFilePath
    val file = myFixture.findFileInTempDir(fileName)
    taskFile.setText(VfsUtilCore.loadText(file))

    FileEditorManager.getInstance(myFixture.project).openFile(file, true)
    try {
      val document = FileDocumentManager.getInstance().getDocument(file)
      for (placeholder in CCTestCase.getPlaceholders(document, true)) {
        taskFile.addAnswerPlaceholder(placeholder)
      }
    } finally {
      FileEditorManager.getInstance(myFixture.project).closeFile(file)
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
    environment: String = "",
    language: Language = PlainTextLanguage.INSTANCE,
    settings: Any = Unit,
    courseProducer: () -> Course = ::EduCourse,
    buildCourse: CourseBuilder.() -> Unit
  ): Course {
    return course(name, language, environment, courseMode, courseProducer, buildCourse).apply {
      createCourseFiles(project, LightPlatformTestCase.getSourceRoot(), settings)
    }
  }

  protected fun getCourse(): Course = StudyTaskManager.getInstance(project).course!!

  protected fun findPlaceholder(lessonIndex: Int, taskIndex: Int, taskFile: String, placeholderIndex: Int): AnswerPlaceholder =
    findTaskFile(lessonIndex, taskIndex, taskFile).answerPlaceholders[placeholderIndex]

  protected fun findTaskFile(lessonIndex: Int, taskIndex: Int, taskFile: String): TaskFile =
    getCourse().lessons[lessonIndex].taskList[taskIndex].taskFiles[taskFile]!!

  protected fun findTask(lessonIndex: Int, taskIndex: Int): Task = findLesson(lessonIndex).taskList[taskIndex]

  protected fun findLesson(lessonIndex: Int): Lesson = getCourse().lessons[lessonIndex]

  protected fun findFileInTask(lessonIndex: Int, taskIndex: Int, taskFilePath: String): VirtualFile {
    return findTask(lessonIndex, taskIndex).getTaskFile(taskFilePath)?.getVirtualFile(project)!!
  }

  protected fun findFile(path: String): VirtualFile =
    LightPlatformTestCase.getSourceRoot().findFileByRelativePath(path) ?: error("Can't find `$path`")

  protected fun Course.findTask(lessonName: String, taskName: String): Task {
    return getLesson(lessonName)?.getTask(taskName) ?: error("Can't find `$taskName` in `$lessonName`")
  }

  protected fun Task.openTaskFileInEditor(taskFilePath: String, placeholderIndex: Int? = null) {
    val taskFile = getTaskFile(taskFilePath) ?: error("Can't find task file `$taskFilePath` in `$name`")
    val file = taskFile.getVirtualFile(project) ?: error("Can't find virtual file for `${taskFile.name}` task")
    myFixture.openFileInEditor(file)
    if (placeholderIndex != null) {
      val placeholder = taskFile.answerPlaceholders[placeholderIndex]
      myFixture.editor.selectionModel.setSelection(placeholder.offset, placeholder.endOffset)
    }
  }

  protected fun Task.createTaskFileAndOpenInEditor(taskFilePath: String, text: String = "") {
    val taskDir = getTaskDir(project) ?: error("Can't find task dir")
    val file = GeneratorUtils.createChildFile(taskDir, taskFilePath, text) ?: error("Failed to create `$taskFilePath` in $taskDir")
    myFixture.openFileInEditor(file)
  }

  protected fun Task.removeTaskFile(taskFilePath: String) {
    require(getTaskFile(taskFilePath) != null) {
      "Can't find `$taskFilePath` task file in ${name} task"
    }
    val taskDir = getTaskDir(project) ?: error("Can't find task dir")
    val file = taskDir.findFileByRelativePath(taskFilePath) ?: error("Can't find `$taskFilePath` in `$taskDir`")
    runWriteAction { file.delete(this) }
  }

  // TODO: set up more items which are enabled in real course project
  // TODO: come up with better name when we set up not only virtual file listeners
  protected inline fun withVirtualFileListener(course: Course, action: () -> Unit) {
    val virtualFileManager = VirtualFileManager.getInstance()

    val listener = if (course.isStudy) UserCreatedFileListener(project) else CCVirtualFileListener(project)
    virtualFileManager.addVirtualFileListener(listener)
    try {
      action()
    } finally {
      virtualFileManager.removeVirtualFileListener(listener)
    }
  }

  protected fun Course.asEduCourse(): EduCourse {
    return copyAs(EduCourse::class.java)
  }

  protected fun Course.asRemote(): EduCourse {
    val remoteCourse = EduCourse()
    remoteCourse.id = 1
    remoteCourse.name = name
    remoteCourse.courseMode = CCUtils.COURSE_MODE
    remoteCourse.items = Lists.newArrayList(items)
    remoteCourse.language = language

    var hasSections = false
    for (item in remoteCourse.items) {
      if (item is Section) {
        item.id = item.index
        for (lesson in item.lessons) {
          lesson.id = lesson.index
          for (task in lesson.taskList) {
            task.id = task.index
          }
        }
        hasSections = true
      }

      if (item is Lesson) {
        item.id = item.index
        for (task in item.taskList) {
          task.id = task.index
        }
      }
    }

    if (!hasSections) {
      remoteCourse.sectionIds = listOf(1)
    }
    remoteCourse.init(null, null, true)
    StudyTaskManager.getInstance(project).course = remoteCourse
    return remoteCourse
  }

  private fun registerConfigurator(
    disposable: Disposable,
    configuratorClass: Class<*>,
    language: Language,
    courseType: String = EduNames.PYCHARM,
    environment: String = EduNames.DEFAULT_ENVIRONMENT
  ) {
    val extension = EducationalExtensionPoint<EduConfigurator<*>>()
    extension.language = language.id
    extension.implementationClass = configuratorClass.name
    extension.courseType = courseType
    extension.environment = environment
    ExtensionPointName.create<EducationalExtensionPoint<*>>(EducationalExtensionPoint.EP_NAME)
      .getPoint(null)
      .registerExtension(extension, disposable)
  }
}
