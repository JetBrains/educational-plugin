package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.FRAMEWORK
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission.Companion.CHECK_IO_MISSION_TASK_TYPE
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE_WITH_FILE_IO
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTaskWithFileIO
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask.Companion.CODE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.NumberTask.Companion.NUMBER_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.StringTask.Companion.STRING_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.UnsupportedTask.Companion.UNSUPPORTED_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATA_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTaskAttempt
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask.Companion.MATCHING_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask.Companion.SORTING_TASK_TYPE
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.getEditor
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.stepik.course.StepikLesson
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TYPE
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTopic
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask.Companion.REMOTE_EDU_TASK_TYPE
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.remoteConfigFileName
import com.jetbrains.edu.learning.yaml.format.*
import com.jetbrains.edu.learning.yaml.format.student.*
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel

object YamlFormatSynchronizer {
  val LOAD_FROM_CONFIG = Key<Boolean>("Edu.loadItem")

  val MAPPER: ObjectMapper by lazy {
    val mapper = YamlMapperBase.MAPPER
    mapper.addMixInsFromProviders()
    mapper
  }

  @VisibleForTesting
  val REMOTE_MAPPER: ObjectMapper by lazy {
    val mapper = YamlMapperBase.REMOTE_MAPPER
    mapper.addRemoteInfoMixInsFromProviders()
    mapper
  }

  @VisibleForTesting
  val STUDENT_MAPPER: ObjectMapper by lazy {
    val mapper = YamlMapperBase.STUDENT_MAPPER
    mapper.addMixInsFromProviders()
    mapper.addStudentMixInsFromProviders()

    mapper
  }

  @VisibleForTesting
  val STUDENT_MAPPER_WITH_ENCRYPTION: ObjectMapper by lazy {
    val mapper = YamlMapperBase.STUDENT_MAPPER_WITH_ENCRYPTION
    mapper.addMixInsFromProviders()
    mapper.addStudentMixInsFromProviders()

    mapper
  }

  private fun ObjectMapper.addMixInsFromProviders() {
    addMixIn(CodeforcesCourse::class.java, CodeforcesCourseYamlMixin::class.java)
    addMixIn(CourseraCourse::class.java, CourseraCourseYamlMixin::class.java)
    addMixIn(CheckiOCourse::class.java, RemoteCourseYamlMixin::class.java)
    addMixIn(HyperskillCourse::class.java, RemoteCourseYamlMixin::class.java)
    addMixIn(StepikCourse::class.java, RemoteCourseYamlMixin::class.java)
    addMixIn(StepikLesson::class.java, StepikLessonYamlMixin::class.java)
    addMixIn(CodeTask::class.java, CodeTaskYamlMixin::class.java)

    addSubtypes()
  }

  private fun ObjectMapper.addSubtypes() {
    registerCourseSubtypes()
    registerLessonSubtypes()
    registerTaskSubtypes()
  }

  private fun ObjectMapper.registerCourseSubtypes() {
    registerSubtypes(NamedType(CodeforcesCourse::class.java, CodeforcesNames.CODEFORCES.decapitalize()))
    registerSubtypes(NamedType(CourseraCourse::class.java, CourseraNames.COURSERA.decapitalize()))
    registerSubtypes(NamedType(CheckiOCourse::class.java, CheckiONames.CHECKIO_TYPE.decapitalize()))
    registerSubtypes(NamedType(HyperskillCourse::class.java, HYPERSKILL_TYPE.decapitalize()))
    registerSubtypes(NamedType(StepikCourse::class.java, StepikNames.STEPIK_TYPE.decapitalize()))
  }

  private fun ObjectMapper.registerLessonSubtypes() {
    registerSubtypes(NamedType(StepikLesson::class.java, StepikNames.STEPIK_TYPE))
    registerSubtypes(NamedType(CheckiOStation::class.java, CheckiONames.CHECKIO_LESSON_TYPE))
  }

  private fun ObjectMapper.registerTaskSubtypes() {
    registerSubtypes(NamedType(RemoteEduTask::class.java, REMOTE_EDU_TASK_TYPE))
    registerSubtypes(NamedType(DataTask::class.java, DATA_TASK_TYPE))
    registerSubtypes(NamedType(CodeTask::class.java, CODE_TASK_TYPE))
    registerSubtypes(NamedType(CheckiOMission::class.java, CHECK_IO_MISSION_TASK_TYPE))
    registerSubtypes(NamedType(CodeforcesTask::class.java, CODEFORCES_TASK_TYPE))
    registerSubtypes(NamedType(CodeforcesTaskWithFileIO::class.java, CODEFORCES_TASK_TYPE_WITH_FILE_IO))
    registerSubtypes(NamedType(StringTask::class.java, STRING_TASK_TYPE))
    registerSubtypes(NamedType(NumberTask::class.java, NUMBER_TASK_TYPE))
    registerSubtypes(NamedType(UnsupportedTask::class.java, UNSUPPORTED_TASK_TYPE))
    registerSubtypes(NamedType(MatchingTask::class.java, MATCHING_TASK_TYPE))
    registerSubtypes(NamedType(SortingTask::class.java, SORTING_TASK_TYPE))
  }

  private fun ObjectMapper.addRemoteInfoMixInsFromProviders() {
    addMixIn(CodeforcesCourse::class.java, CodeforcesCourseRemoteInfoYamlMixin::class.java)
    addMixIn(StepikLesson::class.java, StepikLessonRemoteYamlMixin::class.java)
    addMixIn(DataTask::class.java, RemoteDataTaskYamlMixin::class.java)
    addMixIn(DataTaskAttempt::class.java, DataTaskAttemptYamlMixin::class.java)
    addHyperskillMixins()
  }

  private fun ObjectMapper.addHyperskillMixins() {
    addMixIn(HyperskillCourse::class.java, HyperskillCourseMixin::class.java)
    addMixIn(HyperskillProject::class.java, HyperskillProjectMixin::class.java)
    addMixIn(HyperskillStage::class.java, HyperskillStageMixin::class.java)
    addMixIn(HyperskillTopic::class.java, HyperskillTopicMixin::class.java)
  }

  private fun ObjectMapper.addStudentMixInsFromProviders() {
    addMixIn(CheckiOStation::class.java, CheckiOStationYamlMixin::class.java)
    addMixIn(CheckiOMission::class.java, CheckiOMissionYamlMixin::class.java)

    addMixIn(CodeforcesTask::class.java, CodeforcesTaskYamlMixin::class.java)
    addMixIn(CodeforcesTaskWithFileIO::class.java, CodeforcesTaskWithFileIOYamlMixin::class.java)

    addMixIn(RemoteEduTask::class.java, RemoteEduTaskYamlMixin::class.java)
    addMixIn(SortingTask::class.java, SortingTaskYamlMixin::class.java)
    addMixIn(MatchingTask::class.java, MatchingTaskYamlMixin::class.java)
  }

  fun saveAll(project: Project) {
    @NonNls
    val errorMessageToLog = "Attempt to create config files for project without course"
    val course = StudyTaskManager.getInstance(project).course ?: error(errorMessageToLog)
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

  fun saveItem(item: StudyItem, mapper: ObjectMapper = item.course.mapper, configName: String = item.configFileName) {
    val course = item.course

    @NonNls
    val errorMessageToLog = "Failed to find project for course"
    val project = course.project ?: error(errorMessageToLog)
    if (!YamlFormatSettings.shouldCreateConfigFiles(project)) {
      return
    }
    item.saveConfig(project, configName, mapper)
  }

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

  fun saveItemWithRemoteInfo(item: StudyItem) {
    saveItem(item)
    saveRemoteInfo(item)
  }

  private fun saveItemRemoteInfo(item: StudyItem) {
    // we don't want to create remote info files in local courses
    if (item.id > 0 || item is HyperskillCourse) {
      saveItem(item, REMOTE_MAPPER, item.remoteConfigFileName)
    }
  }

  fun startSynchronization(project: Project) {
    if (isUnitTestMode) {
      return
    }

    val disposable = StudyTaskManager.getInstance(project)
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(YamlSynchronizationListener(project), disposable)
    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (isLocalConfigFile(file)) {
          if (project.isStudentProject()) {
            @NonNls
            val errorMessageToLog = "Can't find editor for a file: ${file.name}"
            val editor = file.getEditor(project) ?: error(errorMessageToLog)
            showNoEditingNotification(editor)
            return
          }

          // load item to show editor notification if config file is invalid
          YamlLoader.getInstance(project).loadItem(file, false)
        }
      }
    })
  }

  private fun showNoEditingNotification(editor: Editor) {
    val label = JLabel(EduCoreBundle.message("yaml.editor.notification.configuration.file"))
    label.border = JBUI.Borders.empty(5, 10, 5, 0)

    val panel = JPanel(BorderLayout())
    panel.add(label, BorderLayout.CENTER)
    panel.background = MessageType.WARNING.popupBackground

    editor.headerComponent = panel
  }

  private fun StudyItem.saveConfig(project: Project, configName: String, mapper: ObjectMapper) {
    val dir = getConfigDir(project)

    invokeLater {
      runWriteAction {
        val file = dir.findOrCreateChildData(javaClass, configName)
        try {
          file.putUserData(LOAD_FROM_CONFIG, false)
          if (FileTypeManager.getInstance().getFileTypeByFile(file) == UnknownFileType.INSTANCE) {
            @NonNls
            val errorMessageToLog = "Failed to get extension for file ${file.name}"
            FileTypeManager.getInstance().associateExtension(PlainTextFileType.INSTANCE,
                                                             file.extension ?: error(errorMessageToLog))
          }
          VfsUtil.saveText(file, mapper.writeValueAsString(this))
        }
        finally {
          file.putUserData(LOAD_FROM_CONFIG, true)
        }
      }
    }
  }

  fun isConfigFile(file: VirtualFile): Boolean {
    return isLocalConfigFile(file) || isRemoteConfigFile(file)
  }

  fun isRemoteConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return REMOTE_COURSE_CONFIG == name || REMOTE_SECTION_CONFIG == name || REMOTE_LESSON_CONFIG == name || REMOTE_TASK_CONFIG == name
  }

  fun isLocalConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return COURSE_CONFIG == name || SECTION_CONFIG == name || LESSON_CONFIG == name || TASK_CONFIG == name
  }

  val Course.mapper: ObjectMapper
    get() = if (isStudy) {
      if (isMarketplace) STUDENT_MAPPER_WITH_ENCRYPTION else STUDENT_MAPPER
    }
    else {
      MAPPER
    }
}

val StudyItem.configFileName: String
  get() = when (this) {
    is Course -> COURSE_CONFIG
    is Section -> SECTION_CONFIG
    is Lesson -> LESSON_CONFIG
    is Task -> TASK_CONFIG
    else -> {
      @NonNls
      val errorMessageToLog = "Unknown StudyItem type: ${javaClass.simpleName}"
      error(errorMessageToLog)
    }
  }

fun StudyItem.getConfigDir(project: Project): VirtualFile {
  return if (this is Task && lesson is FrameworkLesson) {
    @NonNls
    val errorMessageToLog = "Config for '$name' task dir in framework lesson not found"
    lesson.getDir(project.courseDir)?.findChild(name) ?: error(errorMessageToLog)
  }
  else {
    getDir(project.courseDir) ?: error("Config for '$this' not found")
  }
}
