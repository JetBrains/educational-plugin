package com.jetbrains.edu.coursecreator.yaml

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
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.LESSON_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.SECTION_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.TASK_CONFIG
import com.jetbrains.edu.coursecreator.yaml.format.*
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task

object YamlFormatSynchronizer {
  val LOAD_FROM_CONFIG = Key<Boolean>("Edu.loadItem")

  @VisibleForTesting
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

  private fun createMapper(): ObjectMapper {
    val yamlFactory = YAMLFactory()
    yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    yamlFactory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
    yamlFactory.enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)

    val mapper = ObjectMapper(yamlFactory)
    mapper.registerKotlinModule()
    mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.disable(MapperFeature.AUTO_DETECT_FIELDS, MapperFeature.AUTO_DETECT_GETTERS, MapperFeature.AUTO_DETECT_IS_GETTERS)

    return mapper
  }

  private fun addMixIns(mapper: ObjectMapper) {
    mapper.addMixIn(Course::class.java, CourseYamlMixin::class.java)
    mapper.addMixIn(Section::class.java, SectionYamlMixin::class.java)
    mapper.addMixIn(Lesson::class.java, LessonYamlMixin::class.java)
    mapper.addMixIn(FrameworkLesson::class.java, FrameworkLessonYamlUtil::class.java)
    mapper.addMixIn(Task::class.java, TaskYamlMixin::class.java)
    mapper.addMixIn(EduTask::class.java, EduTaskYamlMixin::class.java)
    mapper.addMixIn(TaskFile::class.java, TaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyYamlMixin::class.java)
  }

  private fun addRemoteMixIns(mapper: ObjectMapper) {
    mapper.addMixIn(EduCourse::class.java, EduCourseRemoteInfoYamlMixin::class.java)
    mapper.addMixIn(Section::class.java, RemoteStudyItemYamlMixin::class.java)
    mapper.addMixIn(Lesson::class.java, RemoteLessonYamlMixin::class.java)
    mapper.addMixIn(Task::class.java, RemoteStudyItemYamlMixin::class.java)
  }

  @JvmStatic
  fun saveAll(project: Project) {
    val course = StudyTaskManager.getInstance(project).course ?: error("Attempt to create config files for project without course")
    saveItem(course)
    course.visitSections { section -> saveItem(section) }
    course.visitLessons { lesson ->
      lesson.visitTasks { task ->
        saveItem(task)
      }
      saveItem(lesson)
    }

    saveRemoteInfo(course)
  }

  @JvmStatic
  fun saveItem(item: StudyItem) {
    doSaveItem(item, item.configFileName, MAPPER)
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
      doSaveItem(item, item.remoteConfigFileName, REMOTE_MAPPER)
    }
  }

  private fun doSaveItem(item: StudyItem, configFile: String, objectMapper: ObjectMapper) {
    val course = item.course
    if (YamlFormatSettings.isDisabled() || course.isStudy) {
      return
    }

    val project = course.project ?: error("Failed to find project for course")
    val undoManager = UndoManager.getInstance(project)
    if (undoManager.isUndoInProgress || undoManager.isRedoInProgress) {
      ApplicationManager.getApplication().invokeLater {
        item.saveConfigDocument(project, configFile, objectMapper)
      }
    }
    else {
      item.saveConfigDocument(project, configFile, objectMapper)
    }
  }

  @JvmStatic
  fun startSynchronization(project: Project) {
    if (YamlFormatSettings.isDisabled()) {
      return
    }
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(YamlSynchronizationListener(project), project)
  }

  private fun StudyItem.saveConfigDocument(project: Project, configName: String, mapper: ObjectMapper) {
    val dir = getDir(project) ?: error("Failed to save ${javaClass.simpleName} '$name' to config file: directory not found")
    runInEdt {
      runUndoTransparentWriteAction {
        val file = dir.findOrCreateChildData(javaClass, configName)

        file.putUserData(LOAD_FROM_CONFIG, false)
        file.document?.setText(mapper.writeValueAsString(this))
        file.putUserData(LOAD_FROM_CONFIG, true)
      }
    }
  }

  private val StudyItem.configFileName: String
    get() = when (this) {
      is Course -> COURSE_CONFIG
      is Section -> SECTION_CONFIG
      is Lesson -> LESSON_CONFIG
      is Task -> TASK_CONFIG
      else -> error("Unknown StudyItem type: ${javaClass.simpleName}")
    }

  private val StudyItem.remoteConfigFileName: String
    get() = when (this) {
      is Course -> REMOTE_COURSE_CONFIG
      is Section -> REMOTE_SECTION_CONFIG
      is Lesson -> REMOTE_LESSON_CONFIG
      is Task -> REMOTE_TASK_CONFIG
      else -> error("Unknown StudyItem type: ${javaClass.simpleName}")
    }

  private val VirtualFile.document: Document?
    get() = FileDocumentManager.getInstance().getDocument(this)

  @JvmStatic
  fun isConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return COURSE_CONFIG == name || SECTION_CONFIG == name || LESSON_CONFIG == name || TASK_CONFIG == name ||
           REMOTE_COURSE_CONFIG == name || REMOTE_SECTION_CONFIG == name || REMOTE_LESSON_CONFIG == name || REMOTE_TASK_CONFIG == name
  }
}