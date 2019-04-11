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
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.LESSON_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.SECTION_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.TASK_CONFIG
import com.jetbrains.edu.coursecreator.yaml.format.*
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task

object YamlFormatSynchronizer {
  private val LOG = Logger.getInstance(YamlFormatSynchronizer.javaClass)
  private val LOAD_FROM_CONFIG = Key<Boolean>("Edu.loadItem")

  @VisibleForTesting
  val MAPPER: ObjectMapper by lazy {
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
    addMixIns(mapper)

    mapper
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

  @JvmStatic
  fun saveAll(project: Project) {
    val course = StudyTaskManager.getInstance(project).course ?: error("Attempt to create config files for project without course")
    saveItem(course)
    course.visitSections { section -> saveItem(section) }
    course.visitLessons { lesson ->
      lesson.visitTasks {
        saveItem(it)
      }
      saveItem(lesson)
    }
  }

  @JvmStatic
  fun saveItem(item: StudyItem) {
    val course = item.course
    if (YamlFormatSettings.isDisabled() || course.isStudy) {
      return
    }

    val project = course.project ?: error("Failed to find project for course")
    val undoManager = UndoManager.getInstance(project)
    if (undoManager.isUndoInProgress || undoManager.isRedoInProgress) {
      ApplicationManager.getApplication().invokeLater {
        item.saveConfigDocument(project)
      }
    }
    else {
      item.saveConfigDocument(project)
    }
  }

  @JvmStatic
  fun startSynchronization(project: Project) {
    if (YamlFormatSettings.isDisabled()) {
      return
    }
    val configFiles = getAllConfigFiles(project)
    for (file in configFiles) {
      file.addSynchronizationListener(project)
    }

    // create missing files if feature was enabled after project was created
    YamlFormatSynchronizer.saveAll(project)
  }

  private fun StudyItem.saveConfigDocument(project: Project) {
    val dir = getDir(project) ?: error("Failed to save ${javaClass.simpleName} '${name}' to config file: directory not found")
    runUndoTransparentWriteAction {
      val isNewConfigFile = dir.findChild(configFileName) == null
      val file = dir.findOrCreateChildData(javaClass, configFileName)
      if (isNewConfigFile) {
        file.addSynchronizationListener(project)
      }
      file.putUserData(LOAD_FROM_CONFIG, false)
      file.document?.setText(MAPPER.writeValueAsString(this))
      file.putUserData(LOAD_FROM_CONFIG, true)
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

  private fun VirtualFile.addSynchronizationListener(project: Project) {
    document?.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        val loadFromConfig = getUserData(LOAD_FROM_CONFIG) ?: true
        if (loadFromConfig) {
          val configDocument = event.document
          FileDocumentManager.getInstance().saveDocumentAsIs(configDocument)
          YamlLoader.loadItem(project, this@addSynchronizationListener)
          ProjectView.getInstance(project).refresh()
        }
      }
    }, project)
  }

  private val VirtualFile.document: Document?
    get() = FileDocumentManager.getInstance().getDocument(this)

  @JvmStatic
  fun isConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return COURSE_CONFIG == name || LESSON_CONFIG == name || TASK_CONFIG == name || SECTION_CONFIG == name
  }

  private fun getAllConfigFiles(project: Project): List<VirtualFile> {
    val configFiles = mutableListOf<VirtualFile?>()
    val course = StudyTaskManager.getInstance(project).course ?: error("Accessing to config files in non-edu project")

    configFiles.add(getConfigFile(project, course, COURSE_CONFIG))

    course.visitLessons { lesson ->
      configFiles.add(getConfigFile(project, lesson, LESSON_CONFIG))
      lesson.visitTasks {
        configFiles.add(getConfigFile(project, it, TASK_CONFIG))
      }
    }

    course.visitSections { configFiles.add(getConfigFile(project, it, SECTION_CONFIG)) }

    return configFiles.filterNotNull()
  }

  private fun getConfigFile(project: Project, item: StudyItem, configFileName: String): VirtualFile? {
    val itemDir = item.getDir(project)
    itemDir?.findChild(configFileName)?.apply { return this }
    val warning = if (itemDir == null) "Cannot find directory for a ${item.javaClass.simpleName}: ${item.name}"
    else "No config file '$configFileName' in '${itemDir.name}'"
    LOG.warn(warning)
    return null
  }
}