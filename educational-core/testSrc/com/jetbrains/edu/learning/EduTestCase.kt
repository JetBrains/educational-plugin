package com.jetbrains.edu.learning

import com.google.common.collect.Lists
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EducationalExtensionPoint
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.configurators.FakeGradleConfigurator
import com.jetbrains.edu.learning.configurators.FakeGradleHyperskillConfigurator
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.framework.impl.FrameworkLessonManagerImpl
import com.jetbrains.edu.learning.handlers.UserCreatedFileListener
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import okhttp3.mockwebserver.MockResponse
import org.apache.http.HttpStatus
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

@RunWith(JUnit38ClassRunner::class) // TODO: drop the annotation when issue with Gradle test scanning go away
abstract class EduTestCase : BasePlatformTestCase() {

  protected lateinit var testPluginDescriptor: IdeaPluginDescriptor

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    testPluginDescriptor = PluginManager.getPlugins().first { it.pluginId.idString.startsWith("com.jetbrains.edu") }
    StudyTaskManager.getInstance(project).course = null // if a test doesn't set any course, course from the previous test stays incorrectly
    SubmissionsManager.getInstance(project).clear()
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
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE,
                         CodeforcesNames.CODEFORCES_COURSE_TYPE)
    registerConfigurator(myFixture.testRootDisposable, FakeGradleConfigurator::class.java, FakeGradleBasedLanguage)
    registerConfigurator(myFixture.testRootDisposable, FakeGradleHyperskillConfigurator::class.java, FakeGradleBasedLanguage, HYPERSKILL)

    CheckActionListener.reset()
    val connection = project.messageBus.connect(testRootDisposable)
    connection.subscribe(StudyTaskManager.COURSE_SET, object : CourseSetListener {
      override fun courseSet(course: Course) {
        EduDocumentListener.setGlobalListener(project, testRootDisposable)
        connection.disconnect()
      }
    })
    val frameworkLessonManagerImpl = FrameworkLessonManager.getInstance(project) as FrameworkLessonManagerImpl
    frameworkLessonManagerImpl.storage = FrameworkLessonManagerImpl.createStorage(project)
    createCourse()
    project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, true)
  }

  override fun tearDown() {
    try {
      (EduBrowser.getInstance() as MockEduBrowser).lastVisitedUrl = null
      SubmissionsManager.getInstance(project).clear()

      val storage = (FrameworkLessonManager.getInstance(project) as FrameworkLessonManagerImpl).storage
      Disposer.dispose(storage)
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

  @Throws(IOException::class)
  private fun createTask(lessonIndex: Int, taskIndex: Int): Task {
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
    taskFile.name = taskFilePath
    task.addTaskFile(taskFile)

    val fileName = "lesson" + lessonIndex + "/" + task.name + "/" + taskFilePath
    val file = myFixture.findFileInTempDir(fileName)
    taskFile.setText(VfsUtilCore.loadText(file))

    FileEditorManager.getInstance(myFixture.project).openFile(file, true)
    try {
      val document = FileDocumentManager.getInstance().getDocument(file)!!
      for (placeholder in getPlaceholders(document, true)) {
        taskFile.addAnswerPlaceholder(placeholder)
      }
    }
    finally {
      FileEditorManager.getInstance(myFixture.project).closeFile(file)
    }
    taskFile.sortAnswerPlaceholders()
  }

  /**
   * Be aware: this method overrides any selection in editor
   * because [com.intellij.testFramework.fixtures.CodeInsightTestFixture.configureFromExistingVirtualFile] loads selection and caret from markup in text
   */
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
    courseMode: CourseMode = CourseMode.STUDENT,
    id: Int? = null,
    description: String = "Test Course Description",
    environment: String = "",
    language: Language = PlainTextLanguage.INSTANCE,
    settings: Any = Unit,
    courseVendor: Vendor? = null,
    courseProducer: () -> Course = ::EduCourse,
    createYamlConfigs: Boolean = false,
    buildCourse: CourseBuilder.() -> Unit
  ): Course {
    val course = course(name, language, description, environment, courseMode, courseProducer, buildCourse).apply {
      vendor = courseVendor
      createCourseFiles(project, module, settings = settings)
      if (createYamlConfigs) {
        createConfigFiles(project)
      }
    }
    if (id != null) {
      course.id = id
    }

    SubmissionsManager.getInstance(project).course = course
    course.init(false)
    return course
  }

  protected fun getCourse(): Course = StudyTaskManager.getInstance(project).course!!

  protected fun findPlaceholder(lessonIndex: Int, taskIndex: Int, taskFile: String, placeholderIndex: Int): AnswerPlaceholder =
    findTaskFile(lessonIndex, taskIndex, taskFile).answerPlaceholders[placeholderIndex]

  protected fun findTaskFile(lessonIndex: Int, taskIndex: Int, taskFile: String): TaskFile =
    getCourse().lessons[lessonIndex].taskList[taskIndex].taskFiles[taskFile]!!

  protected fun findTask(lessonIndex: Int, taskIndex: Int): Task = findLesson(lessonIndex).taskList[taskIndex]

  protected fun findTask(sectionIndex: Int,
                         lessonIndex: Int,
                         taskIndex: Int): Task = getCourse().sections[sectionIndex].lessons[lessonIndex].taskList[taskIndex]

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
    val taskDir = getDir(project.courseDir) ?: error("Can't find task dir")
    val file = GeneratorUtils.createChildFile(project, taskDir, taskFilePath, text) ?: error("Failed to create `$taskFilePath` in $taskDir")
    myFixture.openFileInEditor(file)
  }

  protected fun Task.removeTaskFile(taskFilePath: String) {
    require(getTaskFile(taskFilePath) != null) {
      "Can't find `$taskFilePath` task file in $name task"
    }
    val taskDir = getDir(project.courseDir) ?: error("Can't find task dir")
    val file = taskDir.findFileByRelativePath(taskFilePath) ?: error("Can't find `$taskFilePath` in `$taskDir`")
    runWriteAction { file.delete(this) }
  }

  // TODO: set up more items which are enabled in real course project
  // TODO: come up with better name when we set up not only virtual file listeners
  protected inline fun withVirtualFileListener(course: Course, action: () -> Unit) {
    val listener = if (course.isStudy) UserCreatedFileListener(project) else CCVirtualFileListener(project)

    val connection = ApplicationManager.getApplication().messageBus.connect()
    connection.subscribe(VirtualFileManager.VFS_CHANGES, listener)
    try {
      action()
    }
    finally {
      connection.disconnect()
    }
  }

  protected fun Course.asEduCourse(): EduCourse {
    return copyAs(EduCourse::class.java)
  }

  protected fun Course.asRemote(courseMode: CourseMode = CourseMode.EDUCATOR): EduCourse {
    val remoteCourse = EduCourse()
    remoteCourse.id = 1
    remoteCourse.name = name
    remoteCourse.courseMode = courseMode
    remoteCourse.items = Lists.newArrayList(items)
    remoteCourse.programmingLanguage = programmingLanguage
    remoteCourse.description = description

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
    remoteCourse.init(true)
    StudyTaskManager.getInstance(project).course = remoteCourse
    return remoteCourse
  }

  private fun registerConfigurator(
    disposable: Disposable,
    configuratorClass: Class<*>,
    language: Language,
    courseType: String = EduNames.PYCHARM,
    environment: String = DEFAULT_ENVIRONMENT
  ) {
    val extension = EducationalExtensionPoint<EduConfigurator<*>>()
    extension.language = language.id
    extension.implementationClass = configuratorClass.name
    extension.courseType = courseType
    extension.environment = environment
    extension.pluginDescriptor = testPluginDescriptor
    EducationalExtensionPoint.EP_NAME.point.registerExtension(extension, disposable)
  }

  protected fun getTestFile(fileName: String) = testDataPath + fileName

  protected fun mockResponse(fileName: String, responseCode: Int = HttpStatus.SC_OK): MockResponse =
    MockResponseFactory.fromFile(getTestFile(fileName), responseCode)

  @Throws(IOException::class)
  protected fun loadText(fileName: String): String = FileUtil.loadFile(File(testDataPath, fileName))

  protected fun checkFileTree(block: FileTreeBuilder.() -> Unit) {
    fileTree(block).assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  companion object {

    @JvmStatic
    fun getPlaceholders(document: Document, useLength: Boolean): List<AnswerPlaceholder> {
      return WriteCommandAction.writeCommandAction(null).compute<List<AnswerPlaceholder>, RuntimeException> {
        val placeholders = mutableListOf<AnswerPlaceholder>()
        val openingTagRx = "<placeholder( taskText=\"(.+?)\")?( possibleAnswer=\"(.+?)\")?( hint=\"(.+?)\")?( hint2=\"(.+?)\")?>"
        val closingTagRx = "</placeholder>"
        val text = document.charsSequence
        val openingMatcher = Pattern.compile(openingTagRx).matcher(text)
        val closingMatcher = Pattern.compile(closingTagRx).matcher(text)
        var pos = 0
        while (openingMatcher.find(pos)) {
          val answerPlaceholder = AnswerPlaceholder()
          val taskText = openingMatcher.group(2)
          if (taskText != null) {
            answerPlaceholder.placeholderText = taskText
            answerPlaceholder.length = taskText.length
          }
          var possibleAnswer = openingMatcher.group(4)
          if (possibleAnswer != null) {
            answerPlaceholder.possibleAnswer = possibleAnswer
          }
          answerPlaceholder.offset = openingMatcher.start()
          if (!closingMatcher.find(openingMatcher.end())) {
            LOG.error("No matching closing tag found")
          }
          var length: Int
          if (useLength) {
            answerPlaceholder.placeholderText = text.substring(openingMatcher.end(), closingMatcher.start())
            answerPlaceholder.length = closingMatcher.start() - openingMatcher.end()
            length = answerPlaceholder.length
          }
          else {
            if (possibleAnswer == null) {
              possibleAnswer = document.getText(TextRange.create(openingMatcher.end(), closingMatcher.start()))
              answerPlaceholder.possibleAnswer = possibleAnswer
              answerPlaceholder.length = possibleAnswer.length
            }
            length = answerPlaceholder.possibleAnswer.length
          }
          document.deleteString(closingMatcher.start(), closingMatcher.end())
          document.deleteString(openingMatcher.start(), openingMatcher.end())
          FileDocumentManager.getInstance().saveDocument(document)
          placeholders.add(answerPlaceholder)
          pos = answerPlaceholder.offset + length
        }
        placeholders
      }
    }
  }

  fun <T> withDefaultHtmlTaskDescription(action: () -> T): T {
    val useHtml = CCSettings.getInstance().useHtmlAsDefaultTaskFormat()
    CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(true)
    return try {
      action()
    }
    finally {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(useHtml)
    }
  }
}
