package com.jetbrains.edu.coursecreator.actions

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.io.ZipUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.mixins.*
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduNames.COURSE_META_FILE
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CourseArchiveCreator(
  private val project: Project,
  private val jsonFolder: VirtualFile,
  private val zipFile: File,
  private val showMessage: Boolean
) : Computable<Boolean> {

  override fun compute(): Boolean? {
    val course = StudyTaskManager.getInstance(project).course ?: return false
    jsonFolder.refresh(false, true)
    val courseCopy = course.copy()
    loadActualTexts(courseCopy)
    courseCopy.sortItems()
    createAdditionalFiles(courseCopy)
    return try {
      val json = generateJson(jsonFolder, courseCopy)
      VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
      packCourse(json, zipFile, showMessage)
      synchronize(project)
      true
    }
    catch (e: IOException) {
      LOG.error("Failed to create course archive", e)
      false
    }
  }

  private fun createAdditionalFiles(course: Course) {
    val lesson = CCUtils.createAdditionalLesson(course, project, EduNames.ADDITIONAL_MATERIALS)
    if (lesson != null) {
      course.addLesson(lesson)
    }
  }

  private fun loadActualTexts(courseCopy: Course) {
    courseCopy.visitLessons { lesson ->
      val lessonDir = lesson.getLessonDir(project)
      if (lessonDir == null) return@visitLessons true
      for (task in lesson.taskList) {
        val taskDir = task.getTaskDir(project) ?: continue
        convertToStudentTaskFiles(task, taskDir)
        addDescriptions(task)
      }
      true
    }
  }

  private fun convertToStudentTaskFiles(task: Task, taskDir: VirtualFile) {
    val studentTaskFiles = LinkedHashMap<String, TaskFile>()
    for ((key, value) in task.taskFiles) {
      val answerFile = EduUtils.findTaskFileInDir(value, taskDir) ?: continue
      val studentFile = EduUtils.createStudentFile(project, answerFile, task)
      if (studentFile != null) {
        studentTaskFiles[key] = studentFile
      }
    }
    task.taskFiles = studentTaskFiles
  }

  private fun addDescriptions(task: Task) {
    val descriptionFile = task.getDescriptionFile(project)

    if (descriptionFile != null) {
      try {
        task.descriptionText = VfsUtilCore.loadText(descriptionFile)
        val extension = descriptionFile.extension
        val descriptionFormat = DescriptionFormat.values().firstOrNull { format -> format.fileExtension == extension }
        if (descriptionFormat != null) {
          task.descriptionFormat = descriptionFormat
        }
      }
      catch (e: IOException) {
        LOG.warn("Failed to load text " + descriptionFile.name)
      }

    }
    else {
      LOG.warn(String.format("Can't find description file for task `%s`", task.name))
    }
  }

  private fun synchronize(project: Project) {
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
    ProjectView.getInstance(project).refresh()
  }

  private fun packCourse(json: File, zipFile: File, showMessage: Boolean) {
    ZipUtil.compressFile(json, zipFile)
    if (showMessage) {
      ApplicationManager.getApplication().invokeLater {
        Messages.showInfoMessage("Course archive was saved to " + zipFile.path,
                                 "Course Archive Was Created Successfully")
      }
    }
  }

  private fun generateJson(parentDir: VirtualFile, course: Course): File {
    val mapper = if (course.id == 0) localCourseMapper else remoteCourseMapper

    val jsonFile = File(File(parentDir.path), COURSE_META_FILE)
    mapper.writer(printer).writeValue(jsonFile, course)
    return jsonFile
  }

  companion object {
    private val LOG = Logger.getInstance(CourseArchiveCreator::class.java.name)

    private val printer: PrettyPrinter?
      get() {
        if (ApplicationManager.getApplication().isUnitTestMode) {
          return null
        }
        val prettyPrinter = DefaultPrettyPrinter()
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
        return prettyPrinter
      }

    private val localCourseMapper: ObjectMapper
      get() {
        val factory = JsonFactory()
        val mapper = ObjectMapper(factory)
        mapper.addMixIn(EduCourse::class.java, LocalEduCourseMixin::class.java)
        mapper.addMixIn(Section::class.java, LocalSectionMixin::class.java)
        mapper.addMixIn(Lesson::class.java, LocalLessonMixin::class.java)
        mapper.addMixIn(Task::class.java, LocalTaskMixin::class.java)
        addCommonMixins(mapper)
        mapper.enable(WRITE_ENUMS_USING_TO_STRING)
        mapper.enable(READ_ENUMS_USING_TO_STRING)
        return mapper
      }

    private val remoteCourseMapper: ObjectMapper
      get() {
        val factory = JsonFactory()
        val mapper = ObjectMapper(factory)
        mapper.addMixIn(EduCourse::class.java, RemoteEduCourseMixin::class.java)
        mapper.addMixIn(Section::class.java, RemoteSectionMixin::class.java)
        mapper.addMixIn(Lesson::class.java, RemoteLessonMixin::class.java)
        mapper.addMixIn(Task::class.java, RemoteTaskMixin::class.java)
        addCommonMixins(mapper)
        mapper.enable(WRITE_ENUMS_USING_TO_STRING)
        mapper.enable(READ_ENUMS_USING_TO_STRING)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a")
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        mapper.dateFormat = dateFormat
        return mapper
      }

    private fun addCommonMixins(mapper: ObjectMapper) {
      mapper.addMixIn(TaskFile::class.java, TaskFileMixin::class.java)
      mapper.addMixIn(AdditionalFile::class.java, AdditionalFileMixin::class.java)
      mapper.addMixIn(FeedbackLink::class.java, FeedbackLinkMixin::class.java)
      mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderMixin::class.java)
      mapper.addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
    }
  }
}
