package com.jetbrains.edu.coursecreator.configuration

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.codeStyle.NameUtil
import com.jetbrains.edu.coursecreator.configuration.mixins.*
import com.jetbrains.edu.learning.EduDocumentListener
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.course
import com.jetbrains.edu.learning.courseFormat.ext.findSourceDir
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.editor.EduEditor
import org.jetbrains.annotations.TestOnly
import java.io.IOException


object CourseInfoSynchronizer {
  private val LOG = Logger.getInstance(CourseInfoSynchronizer.javaClass)
  private val LOAD_FROM_CONFIG = Key<Boolean>("Edu.loadFromConfig")

  private const val COURSE_CONFIG = "course-info.yaml"
  private const val LESSON_CONFIG = "lesson-info.yaml"
  private const val TASK_CONFIG = "task-info.yaml"
  private const val SECTION_CONFIG = "section-info.yaml"

  private val MAPPER: ObjectMapper by lazy {
    val yamlFactory = YAMLFactory()
    yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    yamlFactory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
    yamlFactory.enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)

    val mapper = ObjectMapper(yamlFactory)
    mapper.registerKotlinModule()
    mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    addMixins(mapper)

    mapper
  }

  fun isConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return COURSE_CONFIG == name || LESSON_CONFIG == name || TASK_CONFIG == name
  }

  private fun addMixins(mapper: ObjectMapper) {
    mapper.addMixIn(Course::class.java, CourseYamlMixin::class.java)
    mapper.addMixIn(Lesson::class.java, LessonYamlMixin::class.java)
    mapper.addMixIn(Task::class.java, TaskYamlMixin::class.java)
    mapper.addMixIn(EduTask::class.java, EduTaskYamlMixin::class.java)
    mapper.addMixIn(TaskFile::class.java, TaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderYamlMixin::class.java)
  }


  fun loadFromConfig(project: Project,
                     configFile: VirtualFile,
                     configDocument: Document?,
                     editor: Editor? = configFile.getEditor(project)) {
    if (editor != null) {
      if (editor.headerComponent is InvalidFormatPanel) {
        editor.headerComponent = null
      }
    }
    configDocument ?: return
    val name = configFile.name
    try {
      when (name) {
        COURSE_CONFIG -> loadCourse(project, configDocument.text)
        LESSON_CONFIG -> loadLesson(project, configFile.parent, configDocument.text)
        TASK_CONFIG -> loadTask(project, configFile.parent, configDocument.text)
        else -> throw IllegalStateException("unknown config file: ${configFile.name}")
      }
      FileDocumentManager.getInstance().saveDocumentAsIs(configDocument)
    }
    catch (e: MissingKotlinParameterException) {
      val parameterName = e.parameter.name
      if (parameterName == null) {
        showError(project, configFile, editor)
      }
      else {
        showError(project, configFile, editor,
                  "${StringUtil.join(NameUtil.nameToWordsLowerCase(parameterName), "_")} is empty")
      }
    }
    catch (e: MismatchedInputException) {
      showError(project, configFile, editor)
    }
    catch (e: InvalidYamlFormatException) {
      showError(project, configFile, editor, e.message.capitalize())
    }
    catch (e: IOException) {
      val causeException = e.cause
      if (causeException?.message == null || causeException !is InvalidYamlFormatException) {
        showError(project, configFile, editor)
      }
      else {
        showError(project, configFile, editor, causeException.message.capitalize())
      }
    }
  }

  private fun showError(project: Project, configFile: VirtualFile, editor: Editor?, cause: String = "invalid config") {
    if (editor != null) {
      editor.headerComponent = InvalidFormatPanel(cause)
    }
    else {
      val notification = InvalidConfigNotification(project, configFile, cause)
      notification.notify(project)
    }
  }


  fun saveAll(project: Project) {
    val course = StudyTaskManager.getInstance(project).course
    if (course == null) {
      LOG.error("Attempt to create config files for project without course")
      return
    }
    CourseInfoSynchronizer.saveCourse(project)
    for (lesson in course.lessons) {
      val lessonDir = lesson.getLessonDir(project)
      CourseInfoSynchronizer.saveLesson(lessonDir, lesson)
      for (task in lesson.getTaskList()) {
        CourseInfoSynchronizer.saveTask(task.getTaskDir(project), task)
      }
    }
  }

  fun saveCourse(project: Project) {
    val course = StudyTaskManager.getInstance(project).course!!
    if (course.isStudy) {
      return
    }
    saveToFile(course, project.baseDir, COURSE_CONFIG)
  }

  fun saveLesson(lessonDir: VirtualFile?, lesson: Lesson) {
    if (lesson.course.isStudy) {
      return
    }
    if (lessonDir == null) {
      LOG.error("Failed to save lesson '${lesson.name}' to config file: lesson directory not found")
      return
    }
    saveToFile(lesson, lessonDir, LESSON_CONFIG)
  }

  fun saveTask(taskDir: VirtualFile?, task: Task) {
    if (task.lesson.course.isStudy) {
      return
    }
    if (taskDir == null) {
      LOG.error("Failed to save task '${task.name}' to config file: task directory not found")
      return
    }
    saveToFile(task, taskDir, TASK_CONFIG)
  }

  private fun loadCourse(project: Project, courseInfo: String) {
    if (project.isDisposed) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course!!
    val courseFromInfo = MAPPER.readValue(courseInfo, Course::class.java)
    course.apply {
      course.name = courseFromInfo.name
      course.languageCode = courseFromInfo.languageCode
      course.language = courseFromInfo.language
      course.description = courseFromInfo.description
      for ((index, lessonFromInfo) in courseFromInfo.lessons.withIndex()) {
        val lesson = course.lessons.getOrNull(index) ?: continue
        lesson.name = lessonFromInfo.name
      }
    }
    ProjectView.getInstance(project).refresh()
  }

  private fun loadLesson(project: Project, lessonDir: VirtualFile, lessonInfo: String) {
    if (project.isDisposed) {
      return
    }
    val lesson = findLesson(lessonDir, project)
    if (lesson == null) {
      LOG.info("Failed to synchronize: ${lessonDir.name} not found")
      return
    }
    val lessonFromInfo = MAPPER.readValue(lessonInfo, Lesson::class.java)
    for ((index, taskFromInfo) in lessonFromInfo.taskList.withIndex()) {
      val task = lesson.taskList.getOrNull(index) ?: continue
      task.name = taskFromInfo.name ?: throw InvalidYamlFormatException("unnamed task")
    }
    ProjectView.getInstance(project).refresh()
  }

  private fun findLesson(lessonDir: VirtualFile,
                         project: Project): Lesson? {
    val index = Integer.valueOf(lessonDir.name.substring(EduNames.LESSON.length)) - 1
    return StudyTaskManager.getInstance(project).course!!.lessons.getOrNull(index)
  }

  private fun findTask(taskDir: VirtualFile, project: Project): Task? {
    val lesson = findLesson(taskDir.parent, project) ?: return null
    val index = Integer.valueOf(taskDir.name.substring(EduNames.TASK.length)) - 1
    return lesson.getTaskList().getOrNull(index)
  }

  private fun loadTask(project: Project, taskDir: VirtualFile, taskInfo: String) {
    val task = findTask(taskDir, project)
    if (task == null) {
      LOG.info("Failed to synchronize: ${taskDir.name} not found")
      return
    }
    val course = task.course
    if (course == null) {
      LOG.info("Failed to synchronize ${taskDir.name}: course is null")
      return
    }
    val newTask = deserializeTask(taskInfo, taskDir, course)
    newTask.init(course, task.lesson, false)
    newTask.lesson = task.lesson
    newTask.apply {
      stepId = task.stepId
      index = task.index
      name = task.name
    }
    task.lesson.getTaskList()[task.index - 1] = newTask
    for (file in FileEditorManager.getInstance(project).openFiles) {
      if (VfsUtil.isAncestor(taskDir, file, true)) {
        val taskFile = EduUtils.getTaskFile(project, file) ?: continue
        val selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor(file)
        if (selectedEditor is EduEditor) {
          selectedEditor.taskFile = taskFile
          EduEditor.removeListener(selectedEditor.editor.document)
          EduEditor.addDocumentListener(selectedEditor.editor.document, EduDocumentListener(taskFile, true))
          EduUtils.drawAllAnswerPlaceholders(selectedEditor.editor, taskFile)
        }
      }
    }
  }

  @TestOnly
  fun testTaskLoading(project: Project, taskDir: VirtualFile, taskInfo: String) {
    loadTask(project, taskDir, taskInfo)
  }

  private fun deserializeTask(taskInfo: String,
                              taskDir: VirtualFile,
                              course: Course): Task {
    val treeNode = MAPPER.readTree(taskInfo)
    val type = treeNode.get("type")?.asText()
    if (type == null) {
      throw InvalidYamlFormatException("task type not specified")
    }
    val clazz = when (type) {
      "edu" -> EduTask::class.java
      "output" -> OutputTask::class.java
      "theory" -> TheoryTask::class.java
      "null" -> throw InvalidYamlFormatException("task type not specified")
      else -> throw InvalidYamlFormatException("Unsupported task type '$type'")
    }
    val task = MAPPER.treeToValue(treeNode, clazz)
    if (task is EduTask) {
      updateEduTask(task, taskDir, course)
    }
    return task
  }

  private fun updateEduTask(task: Task,
                            taskDir: VirtualFile,
                            course: Course) {
    val sourceDir = task.findSourceDir(taskDir, course.sourceDir)
    if (sourceDir == null) {
      throw IllegalStateException("Failed to deserialize edu task: source directory not found")
    }
    for (taskFile in task.taskFiles.values) {
      val file = sourceDir.findFileByRelativePath(taskFile.name) ?: continue
      val document = FileDocumentManager.getInstance().getDocument(file) ?: continue
      for (answerPlaceholder in taskFile.answerPlaceholders) {
        try {
          val possibleAnswer = document.getText(
            TextRange.create(answerPlaceholder.offset, answerPlaceholder.offset + answerPlaceholder.realLength))
          answerPlaceholder.possibleAnswer = possibleAnswer
        }
        catch (e: IndexOutOfBoundsException) {
          throw InvalidYamlFormatException("invalid placeholder (start = ${answerPlaceholder.offset}, " +
                                           "end = ${answerPlaceholder.offset + answerPlaceholder.realLength}, " +
                                           "file length = ${document.textLength})")
        }
      }
    }
  }

  private fun saveToFile(item: Any, dir: VirtualFile, fileName: String) {
    ApplicationManager.getApplication().runWriteAction {
      val file = dir.findOrCreateChildData(CourseInfoSynchronizer.javaClass, fileName)
      file.putUserData(LOAD_FROM_CONFIG, false)
      val document = file.getDocument() ?: return@runWriteAction
      document.setText(MAPPER.writeValueAsString(item))
      file.putUserData(LOAD_FROM_CONFIG, true)
    }
  }

  private fun getAllConfigFiles(project: Project): List<VirtualFile> {
    val configFiles = mutableListOf<VirtualFile?>()
    val course = StudyTaskManager.getInstance(project).course ?: error("Accessing to config files in non-edu project")
    course.visitLessons { lesson, _ ->
      val lessonDir = lesson.getLessonDir(project)
      if (lesson.section != null) {
        configFiles.add(lessonDir?.parent?.findChild(CourseInfoSynchronizer.SECTION_CONFIG))
      }
      configFiles.add(lessonDir?.findChild(CourseInfoSynchronizer.LESSON_CONFIG))
      lesson.visitTasks { task, _ ->
        configFiles.add(task.getTaskDir(project)?.findChild(CourseInfoSynchronizer.TASK_CONFIG))
        true
      }
      true
    }
    return configFiles.filterNotNull()
  }

  private fun loadAllFromConfigs(project: Project) {
    getAllConfigFiles(project).forEach {
      CourseInfoSynchronizer.loadFromConfig(project, it, it.getDocument())
    }
  }

  fun startSynchronization(project: Project) {
    loadAllFromConfigs(project)
    val configFiles = getAllConfigFiles(project)
    for (file in configFiles) {
      addSynchronizationListener(project, file)
    }
  }

  fun addSynchronizationListener(project: Project, file: VirtualFile) {
    val document = file.getDocument()
    document?.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent?) {
        val loadFromConfig = file.getUserData(LOAD_FROM_CONFIG) ?: true
        if (loadFromConfig) {
          loadFromConfig(project, file, event!!.document)
        }
      }
    }, project)
  }

  private fun VirtualFile.getDocument(): Document? = FileDocumentManager.getInstance().getDocument(this)
  private fun VirtualFile.getEditor(project: Project): Editor? {
    val selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor(this)
    return if (selectedEditor != null && selectedEditor is TextEditor) selectedEditor.editor else null
  }

  @TestOnly
  fun getMapper() = MAPPER
}