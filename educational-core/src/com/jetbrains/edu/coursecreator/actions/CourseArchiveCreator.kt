package com.jetbrains.edu.coursecreator.actions

import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.io.ZipUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.checkIgnoredFiles
import com.jetbrains.edu.coursecreator.CCUtils.generateArchiveFolder
import com.jetbrains.edu.coursecreator.actions.mixins.*
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduNames.COURSE_META_FILE
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.TASK_CONFIG
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.IOException

abstract class CourseArchiveCreator(
  protected val project: Project,
  @NonNls private val location: String,
  protected val aesKey: String?
) : Computable<String?> {

  protected val course: Course? = StudyTaskManager.getInstance(project).course

  @Nls(capitalization = Nls.Capitalization.Sentence)
  override fun compute(): String? {
    val course = course?.copy() ?: return EduCoreBundle.message("error.unable.to.obtain.course.for.project")
    val jsonFolder = generateArchiveFolder(project)
                     ?: return EduCoreBundle.message("error.failed.to.generate.course.archive")

    val error = validateCourse(course)
    if (error != null) {
      if (!isUnitTestMode) {
        LOG.error("Failed to create course archive: $error")
      }
      return error
    }

    try {
      prepareCourse(course)
    }
    catch (e: BrokenPlaceholderException) {
      if (!isUnitTestMode) {
        LOG.error("Failed to create course archive: ${e.message}")
      }
      val yamlFile = e.placeholder.taskFile?.task?.getDir(project.courseDir)?.findChild(TASK_CONFIG) ?: return e.message
      FileEditorManager.getInstance(project).openFile(yamlFile, true)
      return "${e.message}\n\n${e.placeholderInfo}"
    }
    catch (e: HugeBinaryFileException) {
      return e.message
    }
    return try {
      val json = generateJson(jsonFolder, course)
      VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
      ZipUtil.compressFile(json, File(location))
      synchronize(project)
      null
    }
    catch (e: IOException) {
      LOG.error("Failed to create course archive", e)
      EduCoreBundle.message("error.failed.to.write")
    }
  }

  open fun validateCourse(course: Course): String? {
    return checkIgnoredFiles(project)
  }

  @VisibleForTesting
  fun prepareCourse(course: Course) {
    loadActualTexts(project, course)
    course.sortItems()
    course.additionalFiles = CCUtils.collectAdditionalFiles(course, project)
  }

  private fun synchronize(project: Project) {
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
    ProjectView.getInstance(project).refresh()
  }

  private fun generateJson(parentDir: VirtualFile, course: Course): File {
    val mapper = getMapper(course)

    val jsonFile = File(File(parentDir.path), COURSE_META_FILE)
    mapper.writer(printer).writeValue(jsonFile, course)
    return jsonFile
  }

  abstract fun getMapper(course: Course): ObjectMapper

  open fun addStudyItemMixins(mapper: ObjectMapper) {
    mapper.addMixIn(Section::class.java, LocalSectionMixin::class.java)
    mapper.addMixIn(Lesson::class.java, LocalLessonMixin::class.java)
    mapper.addMixIn(FrameworkLesson::class.java, FrameworkLessonMixin::class.java)
    mapper.addMixIn(Task::class.java, LocalTaskMixin::class.java)
    mapper.addMixIn(ChoiceTask::class.java, ChoiceTaskLocalMixin::class.java)
    mapper.addMixIn(ChoiceOption::class.java, ChoiceOptionLocalMixin::class.java)
  }

  companion object {
    private val LOG = Logger.getInstance(CourseArchiveCreator::class.java.name)

    private val printer: PrettyPrinter
      get() {
        val prettyPrinter = DefaultPrettyPrinter()
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
        return prettyPrinter
      }

    fun commonMapperSetup(mapper: ObjectMapper, course: Course) {
      if (course is CourseraCourse) {
        mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderMixin::class.java)
      }
      else {
        mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderWithAnswerMixin::class.java)
      }
      mapper.addMixIn(TaskFile::class.java, TaskFileMixin::class.java)
      mapper.addMixIn(FeedbackLink::class.java, FeedbackLinkMixin::class.java)
      mapper.disable(MapperFeature.AUTO_DETECT_FIELDS)
      mapper.disable(MapperFeature.AUTO_DETECT_GETTERS)
      mapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
    }

    @JvmStatic
    fun loadActualTexts(project: Project, course: Course) {
      course.visitLessons { lesson ->
        val lessonDir = lesson.getDir(project.courseDir)
        if (lessonDir == null) return@visitLessons
        for (task in lesson.taskList) {
          loadActualTexts(project, task)
        }
      }
    }

    @JvmStatic
    fun loadActualTexts(project: Project, task: Task) {
      val taskDir = task.getDir(project.courseDir) ?: return
      convertToStudentTaskFiles(project, task, taskDir)
      addDescriptions(project, task)
    }

    private fun convertToStudentTaskFiles(project: Project, task: Task, taskDir: VirtualFile) {
      val studentTaskFiles = LinkedHashMap<String, TaskFile>()
      for ((key, value) in task.taskFiles) {
        val answerFile = EduUtils.findTaskFileInDir(value, taskDir) ?: continue
        val studentFile = answerFile.toStudentFile(project, task)
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
