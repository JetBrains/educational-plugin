package com.jetbrains.edu.coursecreator.actions

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.io.ZipUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.mixins.*
import com.jetbrains.edu.learning.EduNames.COURSE_META_FILE
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CourseArchiveCreator(
  private val project: Project,
  private val jsonFolder: VirtualFile,
  private val zipFile: File,
  private val showMessage: Boolean
) : Computable<String?> {

  override fun compute(): String? {
    val course = StudyTaskManager.getInstance(project).course ?: return "Unable to obtain course for current project"
    jsonFolder.refresh(false, true)
    val courseCopy = course.copy()
    try {
      loadActualTexts(project, courseCopy)
    }
    catch (e: BrokenPlaceholderException) {
      val file = e.placeholder.taskFile?.getVirtualFile(project) ?: return e.message
      FileEditorManagerEx.getInstanceEx(project).openFile(file, true)
      return e.message
    }
    courseCopy.sortItems()
    courseCopy.additionalFiles = CCUtils.collectAdditionalFiles(courseCopy, project)
    return try {
      val json = generateJson(jsonFolder, courseCopy)
      VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
      packCourse(json, zipFile, showMessage)
      synchronize(project)
      null
    }
    catch (e: IOException) {
      LOG.error("Failed to create course archive", e)
      return "Write operation failed. Please check if write operations are allowed and try again."
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
    val mapper = if (course.id == 0) course.localMapper else course.remoteMapper

    val jsonFile = File(File(parentDir.path), COURSE_META_FILE)
    mapper.writer(printer).writeValue(jsonFile, course)
    return jsonFile
  }

  companion object {
    private val LOG = Logger.getInstance(CourseArchiveCreator::class.java.name)

    private val printer: PrettyPrinter?
      get() {
        val prettyPrinter = DefaultPrettyPrinter()
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
        return prettyPrinter
      }

    private val Course.localMapper: ObjectMapper
      get() {
        val factory = JsonFactory()
        val mapper = ObjectMapper(factory)
        mapper.addMixIn(CourseraCourse::class.java, CourseraCourseMixin::class.java)
        mapper.addMixIn(EduCourse::class.java, LocalEduCourseMixin::class.java)
        mapper.addMixIn(Section::class.java, LocalSectionMixin::class.java)
        mapper.addMixIn(Lesson::class.java, LocalLessonMixin::class.java)
        mapper.addMixIn(Task::class.java, LocalTaskMixin::class.java)
        mapper.addMixIn(ChoiceTask::class.java, ChoiceTaskLocalMixin::class.java)
        mapper.addMixIn(ChoiceOption::class.java, ChoiceOptionLocalMixin::class.java)
        commonSetup(mapper, course)
        return mapper
      }

    private val Course.remoteMapper: ObjectMapper
      get() {
        val factory = JsonFactory()
        val mapper = ObjectMapper(factory)
        mapper.addMixIn(EduCourse::class.java, RemoteEduCourseMixin::class.java)
        mapper.addMixIn(Section::class.java, RemoteSectionMixin::class.java)
        mapper.addMixIn(Lesson::class.java, RemoteLessonMixin::class.java)
        mapper.addMixIn(Task::class.java, RemoteTaskMixin::class.java)
        commonSetup(mapper, course)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        mapper.dateFormat = dateFormat
        return mapper
      }

    private fun commonSetup(mapper: ObjectMapper, course: Course) {
      if (course is CourseraCourse) {
        mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderMixin::class.java)
      }
      else {
        mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderWithAnswerMixin::class.java)
      }
      mapper.addMixIn(TaskFile::class.java, TaskFileMixin::class.java)
      mapper.addMixIn(FeedbackLink::class.java, FeedbackLinkMixin::class.java)
      mapper.addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
      mapper.disable(MapperFeature.AUTO_DETECT_FIELDS)
      mapper.disable(MapperFeature.AUTO_DETECT_GETTERS)
      mapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
    }

    @JvmStatic
    fun loadActualTexts(project: Project, course: Course) {
      course.visitLessons { lesson ->
        val lessonDir = lesson.getLessonDir(project)
        if (lessonDir == null) return@visitLessons
        for (task in lesson.taskList) {
          loadActualTexts(project, task)
        }
      }
    }

    @JvmStatic
    fun loadActualTexts(project: Project, task: Task) {
      val taskDir = task.getTaskDir(project) ?: return
      convertToStudentTaskFiles(project, task, taskDir)
      addDescriptions(project, task)
    }

    private fun convertToStudentTaskFiles(project: Project, task: Task, taskDir: VirtualFile) {
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

    fun addDescriptions(project: Project, task: Task) {
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
  }
}
