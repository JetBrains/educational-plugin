package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.VideoSource
import com.jetbrains.edu.learning.courseFormat.tasks.VideoTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlLoader.getEditor
import com.jetbrains.edu.learning.yaml.format.*
import com.jetbrains.edu.learning.yaml.format.student.*
import java.awt.BorderLayout
import java.util.*
import javax.swing.JLabel
import javax.swing.JPanel

object YamlFormatSynchronizer {
  val LOAD_FROM_CONFIG = Key<Boolean>("Edu.loadItem")

  @JvmStatic
  val MAPPER: ObjectMapper by lazy {
    val mapper = createMapper()
    addMixIns(mapper)

    mapper
  }

  @VisibleForTesting
  val REMOTE_MAPPER: ObjectMapper by lazy {
    val mapper = createMapper()
    addRemoteMixIns(mapper)

    mapper
  }

  @VisibleForTesting
  @JvmStatic
  val STUDENT_MAPPER: ObjectMapper by lazy {
    val mapper = createMapper()
    addMixIns(mapper)
    mapper.addStudentMixIns()

    mapper
  }

  private fun createMapper(): ObjectMapper {
    val yamlFactory = YAMLFactory()
    yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    yamlFactory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
    yamlFactory.enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)

    val mapper = ObjectMapper(yamlFactory)
    mapper.registerKotlinModule()
    mapper.setLocale(Locale.ENGLISH)
    mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.disable(MapperFeature.AUTO_DETECT_FIELDS, MapperFeature.AUTO_DETECT_GETTERS, MapperFeature.AUTO_DETECT_IS_GETTERS)

    return mapper
  }

  private fun addMixIns(mapper: ObjectMapper) {
    mapper.addMixIn(CourseraCourse::class.java, CourseraCourseYamlMixin::class.java)
    mapper.addMixIn(Course::class.java, CourseYamlMixin::class.java)
    mapper.addMixIn(Section::class.java, SectionYamlMixin::class.java)
    mapper.addMixIn(Lesson::class.java, LessonYamlMixin::class.java)
    mapper.addMixIn(FrameworkLesson::class.java, FrameworkLessonYamlMixin::class.java)
    mapper.addMixIn(Task::class.java, TaskYamlMixin::class.java)
    mapper.addMixIn(ChoiceTask::class.java, ChoiceTaskYamlMixin::class.java)
    mapper.addMixIn(ChoiceOption::class.java, ChoiceOptionYamlMixin::class.java)
    mapper.addMixIn(TaskFile::class.java, TaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyYamlMixin::class.java)
  }

  private fun addRemoteMixIns(mapper: ObjectMapper) {
    mapper.addMixIn(EduCourse::class.java, EduCourseRemoteInfoYamlMixin::class.java)
    mapper.addMixIn(Lesson::class.java, RemoteLessonYamlMixin::class.java)
    mapper.addMixIn(StudyItem::class.java, RemoteStudyItemYamlMixin::class.java)
  }

  private fun ObjectMapper.addStudentMixIns() {
    addMixIn(Course::class.java, StudentCourseYamlMixin::class.java)
    addMixIn(CheckiOStation::class.java, CheckiOStationYamlMixin::class.java)
    addMixIn(FrameworkLesson::class.java, StudentFrameworkLessonYamlMixin::class.java)
    addMixIn(Task::class.java, StudentTaskYamlMixin::class.java)
    addMixIn(ChoiceTask::class.java, StudentChoiceTaskYamlMixin::class.java)
    addMixIn(VideoTask::class.java, VideoTaskYamlMixin::class.java)
    addMixIn(VideoSource::class.java, VideoSourceYamlMixin::class.java)
    addMixIn(CheckiOMission::class.java, CheckiOMissionYamlMixin::class.java)
    addMixIn(TaskFile::class.java, StudentTaskFileYamlMixin::class.java)
    addMixIn(AnswerPlaceholder::class.java, StudentAnswerPlaceholderYamlMixin::class.java)
    addMixIn(AnswerPlaceholder.MyInitialState::class.java, InitialStateMixin::class.java)
  }

  @JvmStatic
  fun saveAll(project: Project) {
    val course = StudyTaskManager.getInstance(project).course ?: error("Attempt to create config files for project without course")
    val mapper = course.mapper
    saveItem(course, mapper)
    course.visitSections { section -> saveItem(section, mapper) }
    course.visitLessons { lesson ->
      lesson.visitTasks { task ->
        saveItem(task, mapper)
      }
      saveItem(lesson, mapper)
    }

    saveRemoteInfo(course)
  }

  @JvmOverloads
  @JvmStatic
  fun saveItem(item: StudyItem, mapper: ObjectMapper = item.course.mapper, configName: String = item.configFileName) {
    val course = item.course

    val project = course.project ?: error("Failed to find project for course")
    if (!YamlFormatSettings.shouldCreateConfigFiles(project)) {
      return
    }
    item.saveConfigDocument(project, configName, mapper)
  }

  @JvmStatic
  fun saveRemoteInfo(item: StudyItem) {
    when (item) {
      is ItemContainer -> {
        saveItemRemoteInfo(item)
        item.items.forEach { saveRemoteInfo(it) }
      }
      is Task -> {
        saveItemRemoteInfo(item)
      }
    }
  }

  @JvmStatic
  private fun saveItemRemoteInfo(item: StudyItem) {
    // we don't want to create remote info files in local courses
    if (item.id > 0) {
      saveItem(item, REMOTE_MAPPER, item.remoteConfigFileName)
    }
  }

  @JvmStatic
  fun startSynchronization(project: Project) {
    if (isUnitTestMode) {
      return
    }
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(YamlSynchronizationListener(project), project)
    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (isLocalConfigFile(file)) {
          if (EduUtils.isStudentProject(project)) {
            val editor = file.getEditor(project) ?: error("Can't find editor for a file: ${file.name}")
            showNoEditingNotification(editor)
            return
          }

          // load item to show editor notification if config file is invalid
          YamlLoader.loadItem(project, file)
        }
      }
    })
  }

  private fun showNoEditingNotification(editor: Editor) {
    val label = JLabel("This is a course configuration file. Not intended for manual editing.")
    label.border = JBUI.Borders.empty(5, 10, 5, 0)

    val panel = JPanel(BorderLayout())
    panel.add(label, BorderLayout.CENTER)
    panel.background = MessageType.WARNING.popupBackground

    editor.headerComponent = panel
  }

  private fun StudyItem.saveConfigDocument(project: Project, configName: String, mapper: ObjectMapper) {
    val dir = getConfigDir(project)

    ApplicationManager.getApplication().invokeLater {
      runWriteAction {
        val file = dir.findOrCreateChildData(javaClass, configName)
        try {
          file.putUserData(LOAD_FROM_CONFIG, false)
          if (FileTypeManager.getInstance().getFileTypeByFile(file) == UnknownFileType.INSTANCE) {
            FileTypeManager.getInstance().associateExtension(PlainTextFileType.INSTANCE,
                                                             file.extension ?: error("Failed to get extension for file ${file.name}"))
          }
          file.document?.setText(mapper.writeValueAsString(this))
        }
        finally {
          file.putUserData(LOAD_FROM_CONFIG, true)
        }
      }
    }
  }

  private val VirtualFile.document: Document?
    get() = FileDocumentManager.getInstance().getDocument(this)

  @JvmStatic
  fun isConfigFile(file: VirtualFile): Boolean {
    return isLocalConfigFile(file) || isRemoteConfigFile(file)
  }

  @JvmStatic
  fun isRemoteConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return REMOTE_COURSE_CONFIG == name || REMOTE_SECTION_CONFIG == name || REMOTE_LESSON_CONFIG == name || REMOTE_TASK_CONFIG == name
  }

  @JvmStatic
  fun isLocalConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return COURSE_CONFIG == name || SECTION_CONFIG == name || LESSON_CONFIG == name || TASK_CONFIG == name
  }

  val Course.mapper: ObjectMapper
    get() = if (isStudy) STUDENT_MAPPER else MAPPER
}

val StudyItem.configFileName: String
  get() = when (this) {
    is Course -> COURSE_CONFIG
    is Section -> SECTION_CONFIG
    is Lesson -> LESSON_CONFIG
    is Task -> TASK_CONFIG
    else -> error("Unknown StudyItem type: ${javaClass.simpleName}")
  }

val StudyItem.remoteConfigFileName: String
  get() = when (this) {
    is Course -> REMOTE_COURSE_CONFIG
    is Section -> REMOTE_SECTION_CONFIG
    is Lesson -> REMOTE_LESSON_CONFIG
    is Task -> REMOTE_TASK_CONFIG
    else -> error("Unknown StudyItem type: ${javaClass.simpleName}")
  }

fun StudyItem.getConfigDir(project: Project): VirtualFile {
  return if (this is Task && lesson is FrameworkLesson) {
    lesson.getDir(project)?.findChild(name) ?: error("Config for '$name' task dir in framework lesson not found")
  }
  else {
    getDir(project)
  }
}