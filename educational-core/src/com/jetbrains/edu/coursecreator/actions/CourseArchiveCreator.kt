package com.jetbrains.edu.coursecreator.actions

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.annotations.VisibleForTesting
import com.intellij.externalDependencies.DependencyOnPlugin
import com.intellij.externalDependencies.ExternalDependenciesManager
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.io.ZipUtil
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils
import com.jetbrains.edu.coursecreator.CCUtils.generateArchiveFolder
import com.jetbrains.edu.coursecreator.actions.mixins.*
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduNames.COURSE_META_FILE
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.compatibilityProvider
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.encrypt.EncryptionModule
import com.jetbrains.edu.learning.encrypt.getAesKey
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import com.jetbrains.edu.learning.marketplace.updateCourseItems
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.plugins.PluginInfo
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.TASK_CONFIG
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CourseArchiveCreator(
  private val project: Project,
  @NonNls private val location: String,
  private val aesKey: String = getAesKey()
) : Computable<String?> {

  private val course: Course? = StudyTaskManager.getInstance(project).course

  @Nls(capitalization = Nls.Capitalization.Sentence)
  override fun compute(): String? {
    if (course != null && course.isMarketplace && !isUnitTestMode) {
      course.updateCourseItems()
    }
    val course = course?.copy() ?: return EduCoreBundle.message("error.unable.to.obtain.course.for.project")
    val jsonFolder = generateArchiveFolder(project)
                     ?: return EduCoreBundle.message("error.failed.to.generate.course.archive")

    val error = AdditionalFilesUtils.checkIgnoredFiles(project)
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

  @VisibleForTesting
  fun prepareCourse(course: Course) {
    loadActualTexts(project, course)
    course.sortItems()
    course.additionalFiles = AdditionalFilesUtils.collectAdditionalFiles(course, project)
    course.pluginDependencies = collectCourseDependencies(project, course)
    course.courseMode = CourseMode.STUDENT
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

  @VisibleForTesting
  fun getMapper(course: Course): ObjectMapper {
    val factory = JsonFactory()
    val mapper = ObjectMapper(factory)
    mapper.addStudyItemMixins()
    mapper.registerModule(EncryptionModule(aesKey))
    commonMapperSetup(mapper, course)
    return mapper
  }

  /**
   * @return null if course archive was created successfully, non-empty error message otherwise
   */
  fun createArchive(): String? {
    FileDocumentManager.getInstance().saveAllDocuments()
    return ApplicationManager.getApplication().runWriteAction<String>(this)
  }

  companion object {
    private val LOG = Logger.getInstance(CourseArchiveCreator::class.java.name)

    private val printer: PrettyPrinter
      get() {
        val prettyPrinter = DefaultPrettyPrinter()
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
        return prettyPrinter
      }

    // suppressing deprecation for ObjectMapper#disable()
    @Suppress("DEPRECATION")
    fun commonMapperSetup(mapper: ObjectMapper, course: Course) {
      if (course is CourseraCourse) {
        mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderMixin::class.java)
      }
      else {
        mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderWithAnswerMixin::class.java)
      }
      mapper.addMixIn(PluginInfo::class.java, PluginInfoMixin::class.java)
      mapper.addMixIn(TaskFile::class.java, TaskFileMixin::class.java)
      mapper.addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
      mapper.disable(MapperFeature.AUTO_DETECT_FIELDS)
      mapper.disable(MapperFeature.AUTO_DETECT_GETTERS)
      mapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
    }

    fun ObjectMapper.addStudyItemMixins() {
      addMixIn(EduCourse::class.java, RemoteEduCourseMixin::class.java)
      addMixIn(CourseraCourse::class.java, CourseraCourseMixin::class.java)
      addMixIn(FrameworkLesson::class.java, RemoteFrameworkLessonMixin::class.java)
      addMixIn(ChoiceTask::class.java, ChoiceTaskLocalMixin::class.java)
      addMixIn(ChoiceOption::class.java, ChoiceOptionLocalMixin::class.java)
      addMixIn(Section::class.java, RemoteSectionMixin::class.java)
      addMixIn(Lesson::class.java, RemoteLessonMixin::class.java)
      addMixIn(Task::class.java, RemoteTaskMixin::class.java)
      val mapperDateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH)
      mapperDateFormat.timeZone = TimeZone.getTimeZone("UTC")
      dateFormat = mapperDateFormat
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
        LOG.warn("Can't find description file for task ${task.name}")
      }
    }

    private fun collectCourseDependencies(project: Project, course: Course): List<PluginInfo> {
      val requiredPluginIds = course.compatibilityProvider?.requiredPlugins().orEmpty().mapTo(HashSet()) { it.id.idString }
      return ExternalDependenciesManager.getInstance(project).getDependencies(DependencyOnPlugin::class.java)
        // Compatibility provider may produce different required plugins in different IDEs,
        // for example, `PythonCore` in IDEA Community and `Pythonid` in PyCharm Pro.
        // So exclude them from the plugin archive to keep course compatible with all desired IDEs.
        // Note, it doesn't allow user to start course without required plugins because
        // we still check required plugins from compatibility providers on course creation.
        // See https://youtrack.jetbrains.com/issue/EDU-4514
        .filter { it.pluginId !in requiredPluginIds }
        .map {
          PluginInfo(it.pluginId,
                     PluginManager.getInstance().findEnabledPlugin(PluginId.getId(it.pluginId))?.name ?: it.pluginId,
                     it.minVersion,
                     it.maxVersion)
        }
    }
  }
}
