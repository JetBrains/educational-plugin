package com.jetbrains.edu.coursecreator.configuration

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSettings.LESSON_CONFIG
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSettings.SECTION_CONFIG
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSettings.TASK_CONFIG
import com.jetbrains.edu.coursecreator.configuration.mixins.*
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task


object YamlFormatSynchronizer {
  private val LOG = Logger.getInstance(YamlFormatSynchronizer.javaClass)

  private val MAPPER: ObjectMapper by lazy {
    val yamlFactory = YAMLFactory()
    yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    yamlFactory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
    yamlFactory.enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)

    val mapper = ObjectMapper(yamlFactory)
    mapper.registerKotlinModule()
    mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    addMixIns(mapper)

    mapper
  }

  private fun addMixIns(mapper: ObjectMapper) {
    mapper.addMixIn(Course::class.java, CourseYamlMixin::class.java)
    mapper.addMixIn(Section::class.java, SectionYamlMixin::class.java)
    mapper.addMixIn(Lesson::class.java, LessonYamlMixin::class.java)
    mapper.addMixIn(Task::class.java, TaskYamlMixin::class.java)
    mapper.addMixIn(EduTask::class.java, EduTaskYamlMixin::class.java)
    mapper.addMixIn(TaskFile::class.java, TaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyYamlMixin::class.java)
  }

  private fun VirtualFile.getDocument(): Document? = FileDocumentManager.getInstance().getDocument(this)

  @JvmStatic
  fun saveItem(item: StudyItem, project: Project) {
    if (YamlFormatSettings.isDisabled()) {
      return
    }
    if (item.course.isStudy) {
      return
    }
    val fileName = when (item) {
      is Course -> COURSE_CONFIG
      is Section -> SECTION_CONFIG
      is Lesson -> LESSON_CONFIG
      is Task -> TASK_CONFIG
      else -> error("Unknown StudyItem type: ${item.javaClass.name}")
    }
    val dir = item.getDir(project)
    if (dir == null) {
      LOG.error("Failed to save ${item.javaClass.name} '${item.name}' to config file: directory not found")
      return
    }
    ApplicationManager.getApplication().runWriteAction {
      val file = dir.findOrCreateChildData(YamlFormatSynchronizer.javaClass, fileName)
      val document = file.getDocument() ?: return@runWriteAction
      document.setText(MAPPER.writeValueAsString(item))
    }
  }

  @JvmStatic
  fun saveAll(project: Project) {
    val course = StudyTaskManager.getInstance(project).course
    if (course == null) {
      LOG.error("Attempt to create config files for project without course")
      return
    }
    saveItem(course, project)
    for (item in course.items) {
      saveItem(item, project)
    }
    course.visitLessons(LessonVisitor { lesson ->
      for (task in lesson.getTaskList()) {
        saveItem(task, project)
      }
      saveItem(lesson, project)
      true
    })
  }

  @JvmStatic
  fun isConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return COURSE_CONFIG == name || LESSON_CONFIG == name || TASK_CONFIG == name || SECTION_CONFIG == name
  }
}