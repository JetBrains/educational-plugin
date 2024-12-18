package com.jetbrains.edu.coursecreator.actions

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory
import com.intellij.externalDependencies.DependencyOnPlugin
import com.intellij.externalDependencies.ExternalDependenciesManager
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.coursecreator.CCUtils.saveOpenedDocuments
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSE_META_FILE
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import com.jetbrains.edu.learning.json.addStudyItemMixins
import com.jetbrains.edu.learning.json.encrypt.AES256
import com.jetbrains.edu.learning.json.encrypt.EncryptionModule
import com.jetbrains.edu.learning.json.encrypt.getAesKey
import com.jetbrains.edu.learning.json.mixins.*
import com.jetbrains.edu.learning.json.pathInArchive
import com.jetbrains.edu.learning.json.setDateFormat
import com.jetbrains.edu.learning.marketplace.StudyItemIdGenerator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.TASK_CONFIG
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.VisibleForTesting
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.text.Charsets.UTF_8

class CourseArchiveCreator(
  private val project: Project,
  @NonNls private val location: String,
  private val aesKey: String = getAesKey()
) {

  /**
   * @return null when course archive was created successfully, non-empty error message otherwise
   */
  fun createArchive(course: Course): String? {
    require(project.course == course) {
      "Given course is supposed to be associated with the current project"
    }

    ApplicationManager.getApplication().assertIsDispatchThread()
    saveOpenedDocuments(project)

    if (course.isMarketplace && !isUnitTestMode) {
      ProgressManager.getInstance().runProcessWithProgressSynchronously({
        StudyItemIdGenerator.getInstance(project).generateIdsIfNeeded(course)
      }, EduCoreBundle.message("action.create.course.archive.progress.bar"), false, project)
    }

    course.updateEnvironmentSettings(project)
    val courseCopy = course.copy()

    val courseArchiveIndicator = CourseArchiveIndicator()

    try {
      // We run prepareCourse() in EDT because it calls the VirtualFile.toStudentFile method that replaces placeholders inside the document.
      // Modifying the document requires a write action.
      prepareCourse(courseCopy)
    }
    catch (e: BrokenPlaceholderException) {
      if (!isUnitTestMode) {
        LOG.error("Failed to create course archive: ${e.message}")
      }
      val yamlFile = e.placeholder.taskFile.task.getDir(project.courseDir)?.findChild(TASK_CONFIG) ?: return e.message
      FileEditorManager.getInstance(project).openFile(yamlFile, true)
      return "${e.message}\n\n${e.placeholderInfo}"
    }
    catch (e: HugeBinaryFileException) {
      return e.message
    }
    catch (e: FileNotFoundException) {
      return e.message
    }

    return ProgressManager.getInstance().runProcessWithProgressSynchronously<String?, RuntimeException>({
      try {
        unwrapExceptionCause {
          measureTimeAndLog("Create course archive") {
            doCreateCourseArchive(courseArchiveIndicator, courseCopy)
          }
        }

        null
      }
      catch (e: HugeBinaryFileException) {
        e.message
      }
      catch (e: IOException) {
        LOG.warn("IO Exception during creating a course archive", e)
        val message = EduCoreBundle.message("error.failed.to.generate.course.archive.io.exception")
        val exceptionMessage = e.message

        if (exceptionMessage != null && exceptionMessage != "") {
          message + " " + EduCoreBundle.message("error.failed.to.generate.course.archive.io.exception.additional.message", exceptionMessage)
        }
        else {
          message
        }
      }
      catch (_: ProcessCanceledException) {
        EduCoreBundle.message("error.course.archiving.cancelled.by.user")
      }
    }, EduCoreBundle.message("action.create.course.archive.progress.bar"), true, project)
  }

  private fun doCreateCourseArchive(courseArchiveIndicator: CourseArchiveIndicator, courseCopy: Course) =
    doCreateCourseArchive(courseArchiveIndicator, courseCopy, FileOutputStream(location))

  /**
   * Creates course archive and writes it into OutputStream.
   * Then closes the output stream.
   */
  @VisibleForTesting
  fun doCreateCourseArchive(courseArchiveIndicator: CourseArchiveIndicator, courseCopy: Course, out: OutputStream) {
    val courseDir = project.courseDir
    courseArchiveIndicator.init(courseDir, courseCopy, ProgressManager.getInstance().progressIndicator)

    ZipOutputStream(out).use { outputStream ->
      outputStream.withNewEntry(COURSE_META_FILE) {
        val writer = OutputStreamWriter(outputStream, UTF_8)
        generateJson(writer, courseCopy)
      }

      writeAllFilesToArchive(courseCopy, outputStream, courseArchiveIndicator)

      writeIconToArchive(outputStream)
    }

    synchronize(project)
  }

  private fun writeIconToArchive(outputStream: ZipOutputStream) {
    val iconFile = project.courseDir.findChild(EduFormatNames.COURSE_ICON_FILE)
    iconFile?.inputStream?.use { iconInputStream ->
      outputStream.withNewEntry(EduFormatNames.COURSE_ICON_FILE) {
        iconInputStream.copyTo(outputStream)
      }
    }
  }

  private fun writeAllFilesToArchive(
    courseCopy: Course,
    outputStream: ZipOutputStream,
    courseArchiveIndicator: CourseArchiveIndicator
  ) {
    courseCopy.visitEduFiles { eduFile ->
      outputStream.withNewEntry(eduFile.pathInArchive) {
        val bytes = when (val contents = eduFile.contents) {
          is BinaryContents -> contents.bytes
          is TextualContents -> contents.text.toByteArray(UTF_8)
          is UndeterminedContents -> throw IllegalStateException("All contents must be disambiguated before writing archive")
        }
        outputStream.write(AES256.encrypt(bytes, aesKey))
        courseArchiveIndicator.writeFile(eduFile)
      }
    }
  }

  @Throws(IOException::class)
  private fun ZipOutputStream.withNewEntry(name: String, action: () -> Unit) {
    try {
      putNextEntry(ZipEntry(name))
      action()
    }
    finally {
      closeEntry()
    }
  }

  @VisibleForTesting
  fun prepareCourse(course: Course) {
    loadActualTexts(project, course)
    course.sortItems()
    course.pluginDependencies = collectCourseDependencies(project, course)
    course.courseMode = CourseMode.STUDENT
  }

  private fun synchronize(project: Project) {
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
    ProjectView.getInstance(project).refresh()
  }

  private fun generateJson(out: Writer, course: Course) {
    val mapper = getMapper(course)
    mapper.writer(printer).writeValue(out, course)
  }

  @VisibleForTesting
  fun getMapper(course: Course): ObjectMapper {
    val module = SimpleModule()
      .addSerializer(EduCourse::class.java, EduCoursePluginVersionSerializer())

    val mapper = JsonMapper.builder()
      .addModule(module)
      .addModule(EncryptionModule(aesKey))
      .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
      .commonMapperSetup(course)
      .setDateFormat()
      .build()
    mapper.addStudyItemMixins()

    return mapper
  }

  /**
   * Sometimes when a user cancels a process, the ProcessCanceledException becomes a cause of another exception.
   * We need to get rid of the caused exception to see that the process was actually cancelled.
   *
   * Another case is the JsonMappingException, it wraps other exceptions that were a real cause and are interesting for us.
   */
  private fun unwrapExceptionCause(action: () -> Unit) = try {
    action()
  }
  catch (e: JsonMappingException) {
    throw e.cause ?: e
  }
  catch (e: Exception) {
    val cause = e.cause
    if (cause is ProcessCanceledException) throw cause
    throw e
  }

  class EduCoursePluginVersionSerializer : JsonSerializer<EduCourse>() {
    override fun serialize(value: EduCourse, jgen: JsonGenerator, provider: SerializerProvider) {
      jgen.writeStartObject()
      val javaType = provider.constructType(value::class.java)
      val beanDesc: BeanDescription = provider.config.introspect(javaType)
      val serializer: JsonSerializer<Any> =
        BeanSerializerFactory.instance.findBeanOrAddOnSerializer(provider, javaType, beanDesc,
                                                                 provider.isEnabled(MapperFeature.USE_STATIC_TYPING))
      serializer.unwrappingSerializer(null).serialize(value, jgen, provider)
      jgen.addPluginVersion()
      jgen.writeEndObject()
    }

    private fun JsonGenerator.addPluginVersion() {
      val fieldName = JsonMixinNames.PLUGIN_VERSION
      val pluginVersion = if (isUnitTestMode) TEST_PLUGIN_VERSION else pluginVersion(EduNames.PLUGIN_ID) ?: "unknown"
      writeObjectField(fieldName, pluginVersion)
    }
  }

  companion object {
    private val LOG = Logger.getInstance(CourseArchiveCreator::class.java.name)
    private const val TEST_PLUGIN_VERSION = "yyyy.2-yyyy.1-TEST"

    private val printer: PrettyPrinter
      get() {
        val prettyPrinter = DefaultPrettyPrinter()
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
        return prettyPrinter
      }

    fun JsonMapper.Builder.commonMapperSetup(course: Course): JsonMapper.Builder {
      if (course is CourseraCourse) {
        addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderMixin::class.java)
      }
      else {
        addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderWithAnswerMixin::class.java)
      }
      addMixIn(PluginInfo::class.java, PluginInfoMixin::class.java)
      addMixIn(EduFile::class.java, EduFileMixin::class.java)
      addMixIn(TaskFile::class.java, TaskFileMixin::class.java)
      addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
      disable(MapperFeature.AUTO_DETECT_FIELDS)
      disable(MapperFeature.AUTO_DETECT_GETTERS)
      disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
      return this
    }

    fun loadActualTexts(project: Project, course: Course) {
      val courseDir = project.courseDir

      course.visitLessons { lesson ->
        val lessonDir = lesson.getDir(courseDir)
        if (lessonDir == null) return@visitLessons
        for (task in lesson.taskList) {
          loadActualTexts(project, task)
        }
      }

      for (additionalFile in course.additionalFiles) {
        val fsFile = courseDir.findFileByRelativePath(additionalFile.name)
        if (fsFile == null) {
          throw FileNotFoundException(EduCoreBundle.message("error.additional.file.does.not.exist", additionalFile.name))
        }
        additionalFile.contents = when(additionalFile.isBinary) {
          false -> TextualContentsFromDisk(fsFile)
          true -> BinaryContentsFromDisk(fsFile)
          // Undetermined contents seem impossible because additional files
          // are created and read from course-info.yaml always with determined contents.
          // But if the contents are anyhow undetermined (probably because of tests), we must disambiguate it.
          null -> if (fsFile.isToEncodeContent) {
            BinaryContentsFromDisk(fsFile)
          }
          else {
            TextualContentsFromDisk(fsFile)
          }
        }
      }
    }

    private fun loadActualTexts(project: Project, task: Task) {
      val taskDir = task.getDir(project.courseDir) ?: return
      convertToStudentTaskFiles(project, task, taskDir)
      task.updateDescriptionTextAndFormat(project)
    }

    private fun convertToStudentTaskFiles(project: Project, task: Task, taskDir: VirtualFile) {
      val studentTaskFiles = LinkedHashMap<String, TaskFile>()
      for ((key, value) in task.taskFiles) {
        val answerFile = value.findTaskFileInDir(taskDir) ?: continue

        val studentFile = answerFile.toStudentFile(project, task)
        if (studentFile != null) {
          studentTaskFiles[key] = studentFile
        }
      }
      task.taskFiles = studentTaskFiles
    }

    private fun collectCourseDependencies(project: Project, course: Course): List<PluginInfo> {
      val requiredPluginIds = course.compatibilityProvider?.requiredPlugins().orEmpty().mapTo(HashSet()) { it.stringId }
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
