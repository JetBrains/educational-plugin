package com.jetbrains.edu.learning.courseGeneration

import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiModifier
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat.HTML
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat.MD
import com.jetbrains.edu.learning.courseFormat.ext.dirName
import com.jetbrains.edu.learning.courseFormat.ext.isFrameworkTask
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.apache.commons.codec.binary.Base64
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicReference

object GeneratorUtils {

  private val LOG: Logger = Logger.getInstance(GeneratorUtils::class.java)

  private val UNIX_INVALID_SYMBOLS: Regex = "[/:]".toRegex()
  private val WINDOWS_INVALID_SYMBOLS: Regex = "[/\\\\:<>\"?*|;&]".toRegex()

  @Throws(IOException::class)
  @JvmStatic
  fun createCourse(course: Course,
                   baseDir: VirtualFile,
                   indicator: ProgressIndicator) {
    indicator.isIndeterminate = false
    indicator.fraction = 0.0

    val items = course.items
    for ((i, item) in items.withIndex()) {
      indicator.fraction = (i + 1).toDouble() / items.size

      if (item is Lesson) {
        if (!item.isAdditional) {
          indicator.text = "Generating lesson ${i + 1} from ${items.size}"
        }
        else {
          indicator.text = "Generating additional files"
        }
        createLesson(item, baseDir)
      }
      else if (item is Section) {
        indicator.text = "Generating section ${i + 1} from ${items.size}"
        createSection(item, baseDir)
      }
    }
    course.removeAdditionalLesson()
  }

  fun createSection(item: Section, baseDir: VirtualFile) {
    val sectionDir = createUniqueDir(baseDir, item)

    for (lesson in item.lessons) {
      createLesson(lesson, sectionDir)
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createLesson(lesson: Lesson, courseDir: VirtualFile) {
    if (lesson.isAdditional) {
      createAdditionalFiles(lesson, courseDir)
    } else {
      val lessonDir = createUniqueDir(courseDir, lesson)
      val taskList = lesson.getTaskList()
      val isStudy = lesson.course.isStudy
      for ((i, task) in taskList.withIndex()) {
        // We don't want to create task only when:
        // 1. Course is in student mode. In CC mode we always want to create full course structure
        // 2. Lesson is framework lesson. For general lessons we create all tasks because their contents are independent (almost)
        // 3. It's not first task of framework lesson. We create only first task of framework lesson as an entry point of lesson content
        if (!isStudy || lesson !is FrameworkLesson || i == 0) {
          createTask(task, lessonDir)
        }
      }
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createTask(task: Task, lessonDir: VirtualFile) {
    val taskDir = createUniqueDir(lessonDir, task)
    createTaskContent(task, taskDir)
  }

  @Throws(IOException::class)
  private fun createTaskContent(task: Task, taskDir: VirtualFile) {
    for ((_, taskFileContent) in task.taskFiles) {
      createTaskFile(taskDir, taskFileContent)
    }
    createFiles(taskDir, task.testsText)
    createFiles(taskDir, task.additionalFiles.mapValues { (_, file) -> file.getText() })
    val course = task.course
    if (CCUtils.COURSE_MODE == course.courseMode) {
      createDescriptionFile(taskDir, task)
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  private fun createTaskFile(taskDir: VirtualFile, taskFile: TaskFile) {
    createChildFile(taskDir, taskFile.name, taskFile.getText())
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createDescriptionFile(taskDir: VirtualFile, task: Task): VirtualFile? {
    val descriptionFileName = when (task.descriptionFormat) {
      HTML -> EduNames.TASK_HTML
      MD -> EduNames.TASK_MD
      else -> {
        LOG.warn("Description format for task `${task.name}` is null. Use html format")
        EduNames.TASK_HTML
      }
    }
    return createChildFile(taskDir, descriptionFileName, task.descriptionText)
  }

  @Throws(IOException::class)
  private fun createFiles(taskDir: VirtualFile, texts: Map<String, String>) {
    for ((name, value) in texts) {
      val virtualTaskFile = taskDir.findChild(name)
      if (virtualTaskFile == null) {
        createChildFile(taskDir, name, value)
      }
    }
  }

  @Throws(IOException::class)
  private fun createAdditionalFiles(lesson: Lesson, courseDir: VirtualFile) {
    val filesToCreate = additionalFilesToCreate(lesson)

    createFiles(courseDir, filesToCreate)
  }

  fun additionalFilesToCreate(lesson: Lesson): Map<String, String> {
    if (!lesson.isAdditional) {
      return emptyMap()
    }

    val task = lesson.taskList.singleOrNull() ?: return emptyMap()
    val filesToCreate = HashMap(task.testsText)
    task.taskFiles.mapValuesTo(filesToCreate) { entry -> entry.value.getText() }
    filesToCreate.putAll(task.additionalFiles.mapValues { (_, file) -> file.getText() })
    return filesToCreate
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createChildFile(parentDir: VirtualFile, path: String, text: String): VirtualFile? {
    return runInWriteActionAndWait(ThrowableComputable {
      var newDirectories: String? = null
      var fileName = path
      var dir: VirtualFile? = parentDir
      if (path.contains("/")) {
        val pos = path.lastIndexOf("/")
        fileName = path.substring(pos + 1)
        newDirectories = path.substring(0, pos)
      }
      if (newDirectories != null) {
        dir = VfsUtil.createDirectoryIfMissing(parentDir, newDirectories)
      }
      if (dir != null) {
        val virtualTaskFile = dir.findOrCreateChildData(parentDir, fileName)
        if (EduUtils.isImage(path)) {
          virtualTaskFile.setBinaryContent(Base64.decodeBase64(text))
        } else {
          VfsUtil.saveText(virtualTaskFile, text)
        }
        virtualTaskFile
      } else {
        null
      }
    })
  }

  @Throws(IOException::class)
  @JvmStatic
  fun <T> runInWriteActionAndWait(action: ThrowableComputable<T, IOException>): T {
    val application = ApplicationManager.getApplication()
    val resultRef = AtomicReference<T>()
    val exceptionRef = AtomicReference<IOException>()
    application.invokeAndWait {
      application.runWriteAction {
        try {
          resultRef.set(action.compute())
        } catch (e: IOException) {
          exceptionRef.set(e)
        }
      }
    }
    return if (exceptionRef.get() != null) {
      throw IOException(exceptionRef.get())
    } else {
      resultRef.get()
    }
  }

  @JvmStatic
  fun initializeCourse(project: Project, course: Course) {
    course.init(null, null, false)

    if (updateTaskFilesNeeded(course)) {
      updateJavaCodeTaskFileNames(project, course)
    }
    StudyTaskManager.getInstance(project).course = course
  }

  private fun updateTaskFilesNeeded(course: Course): Boolean {
    return course is RemoteCourse && course.isStudy() && EduNames.JAVA == course.getLanguageID()
  }

  private fun updateJavaCodeTaskFileNames(project: Project, course: Course) {
    course.visitLessons { lesson ->
      for (task in lesson.getTaskList()) {
        if (task is CodeTask) {
          for (taskFile in task.getTaskFiles().values) {
            nameTaskFileAfterContainingClass(project, task, taskFile)
          }
        }
      }
      true
    }
  }

  private fun nameTaskFileAfterContainingClass(project: Project, task: Task, taskFile: TaskFile) {
    val language = task.course.languageById
    val fileType = language?.associatedFileType
    if (fileType == null) {
      LOG.warn("Cannot rename task file. Unable to find associated file type for language: " + language.id)
      return
    }

    task.taskFiles.remove(taskFile.name)
    val pathPrefix = taskFile.name.substringBeforeLast(VfsUtilCore.VFS_SEPARATOR_CHAR, "")
    val className = publicClassName(project, taskFile, fileType) + "." + fileType.defaultExtension
    taskFile.name = joinPaths(pathPrefix, className)
    task.addTaskFile(taskFile)
  }

  private fun publicClassName(project: Project, taskFile: TaskFile, fileType: LanguageFileType): String {
    var fileName = "Main"
    val file = PsiFileFactory.getInstance(project).createFileFromText(taskFile.name, fileType, taskFile.getText())
    if (file is PsiClassOwner) {
      val classes = file.classes
      for (aClass in classes) {
        val className = aClass.name
        val isPublic = aClass.hasModifierProperty(PsiModifier.PUBLIC) || fileName == className
        if (isPublic && className != null) {
          fileName = className
          break
        }
      }
    }

    return fileName
  }

  /**
   * Non unique lesson/task/section names can be received from stepik
   */
  @JvmStatic
  fun getUniqueValidName(parentDir: VirtualFile, name: String): String {
    val validName = name.convertToValidName()
    var index = 0
    var candidateName = validName
    while (parentDir.findChild(candidateName) != null) {
      index++
      candidateName = "$validName ($index)"
    }
    return candidateName
  }

  private fun String.convertToValidName(): String {
    val invalidSymbols = if (SystemInfo.isWindows) WINDOWS_INVALID_SYMBOLS else UNIX_INVALID_SYMBOLS
    return replace(invalidSymbols, " ").trim()
  }

  private fun createUniqueDir(parentDir: VirtualFile, item: StudyItem): VirtualFile {
    val (baseDirName, needUpdateItem) = if (item is Task && item.isFrameworkTask && item.course.isStudy)  {
      item.dirName to false
    } else {
      item.name to true
    }

    val uniqueDirName = getUniqueValidName(parentDir, baseDirName)
    if (uniqueDirName != baseDirName && needUpdateItem) {
      item.customPresentableName = item.name
      item.name = uniqueDirName
    }
    return runInWriteActionAndWait(ThrowableComputable {
      VfsUtil.createDirectoryIfMissing(parentDir, uniqueDirName)
    })
  }

  @JvmStatic
  fun createDefaultFile(course: Course, baseName: String, baseText: String): DefaultFileProperties {
    val language = course.languageById
    val extensionSuffix = language?.associatedFileType?.defaultExtension?.let { ".$it" } ?: ""
    val lineCommentPrefix = if (language != null) {
      LanguageCommenters.INSTANCE.forLanguage(language)?.lineCommentPrefix ?: ""
    } else {
      ""
    }
    return DefaultFileProperties("$baseName$extensionSuffix", "$lineCommentPrefix$baseText")
  }

  @JvmStatic
  fun joinPaths(prefix: String?, suffix: String): String {
    return if (prefix.isNullOrEmpty()) suffix else "$prefix${VfsUtilCore.VFS_SEPARATOR_CHAR}$suffix"
  }

  data class DefaultFileProperties(val name: String, val text: String)
}
