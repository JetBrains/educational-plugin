package com.jetbrains.edu.learning.courseGeneration

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modifyModules
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.io.ReadOnlyAttributeUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.LESSON
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.SECTION
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK
import com.jetbrains.edu.learning.courseFormat.ext.dirName
import com.jetbrains.edu.learning.courseFormat.ext.getPathToChildren
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.shouldBeEmpty
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.IdeaDirectoryUnpackMode.*
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

@Suppress("UnstableApiUsage")
object GeneratorUtils {

  private val INVALID_SYMBOLS: Regex = """[/\\:<>"?*|;&]""".toRegex()
  private val INVALID_TRAILING_SYMBOLS: CharArray = charArrayOf(' ', '.', '!')

  @RequiresBlockingContext
  @Throws(IOException::class)
  fun createCourse(
    holder: CourseInfoHolder<Course>,
    indicator: ProgressIndicator
  ) {
    val course = holder.course
    indicator.isIndeterminate = false

    val items = course.items
    for ((i, item) in items.withIndex()) {
      indicator.fraction = (i + 1).toDouble() / items.size

      if (item is Lesson) {
        indicator.text = EduCoreBundle.message("generate.lesson.progress.text", i + 1, items.size)
        createLesson(holder, item, holder.courseDir)
      }
      else if (item is Section) {
        indicator.text = EduCoreBundle.message("generate.section.progress.text", i + 1, items.size)
        createSection(holder, item, holder.courseDir)
      }
    }
    indicator.text = EduCoreBundle.message("generate.additional.files.progress.text")
    unpackAdditionalFiles(holder, ALL_EXCEPT_IDEA_DIRECTORY)

    EduCounterUsageCollector.studyItemCreatedCC(course)
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  fun createSection(project: Project, item: Section, baseDir: VirtualFile): VirtualFile {
    return createSection(project.toCourseInfoHolder(), item, baseDir)
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  private fun createSection(holder: CourseInfoHolder<out Course?>, item: Section, baseDir: VirtualFile): VirtualFile {
    val parentDir = runInWriteActionAndWait {
      VfsUtil.createDirectoryIfMissing(baseDir, item.parent.getPathToChildren())
    }
    val sectionDir = createUniqueDir(parentDir, item)

    for (lesson in item.lessons) {
      createLesson(holder, lesson, sectionDir)
    }
    EduCounterUsageCollector.studyItemCreatedCC(item)
    return sectionDir
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  fun createLesson(project: Project, lesson: Lesson, parentDir: VirtualFile): VirtualFile {
    return createLesson(project.toCourseInfoHolder(), lesson, parentDir)
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  private fun createLesson(holder: CourseInfoHolder<out Course?>, lesson: Lesson, baseDir: VirtualFile): VirtualFile {
    val parentDir = runInWriteActionAndWait {
      VfsUtil.createDirectoryIfMissing(baseDir, lesson.parent.getPathToChildren())
    }

    val lessonDir = createUniqueDir(parentDir, lesson)
    val taskList = lesson.taskList
    for (task in taskList) {
      createTask(holder, task, lessonDir)
    }
    EduCounterUsageCollector.studyItemCreatedCC(lesson)
    return lessonDir
  }

  @Throws(IOException::class)
  fun createTask(project: Project, task: Task, lessonDir: VirtualFile): VirtualFile {
    return createTask(project.toCourseInfoHolder(), task, lessonDir)
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  private fun createTask(holder: CourseInfoHolder<out Course?>, task: Task, lessonDir: VirtualFile): VirtualFile {
    val isFirstInFrameworkLesson = task.parent is FrameworkLesson && task.index == 1
    val isStudyCourse = task.course.isStudy
    val (contentDir, configDir) = if (isStudyCourse && isFirstInFrameworkLesson) {
      // create config dir for yaml files and task description files
      val configDir = createUniqueDir(lessonDir, task)
      // create content dir with specific for framework lesson task name
      val contentDir = createUniqueDir(lessonDir, task, task.dirName, false)
      contentDir to configDir
    }
    else {
      val taskDir = createUniqueDir(lessonDir, task)
      taskDir to taskDir
    }

    if (!isStudyCourse || task.parent !is FrameworkLesson || isFirstInFrameworkLesson) {
      createTaskContent(holder, task, contentDir)
    }

    createDescriptionFile(holder, configDir, task)
    EduCounterUsageCollector.studyItemCreatedCC(task)
    return contentDir
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  fun createTaskContent(project: Project, task: Task, taskDir: VirtualFile) {
    createTaskContent(project.toCourseInfoHolder(), task, taskDir)
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  private fun createTaskContent(holder: CourseInfoHolder<out Course?>, task: Task, taskDir: VirtualFile) {
    val (testFiles, taskFiles) = task.taskFiles.values.partition { task.shouldBeEmpty(it.name) }

    for (file in taskFiles) {
      createChildFile(holder, taskDir, file.name, file.contents, file.isEditable)
    }

    for (file in testFiles) {
      createChildFile(holder, taskDir, file.name, TextualContents.EMPTY, file.isEditable)
    }
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  fun createDescriptionFile(project: Project, taskDir: VirtualFile, task: Task): VirtualFile? {
    return createDescriptionFile(project.toCourseInfoHolder(), taskDir, task)
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  private fun createDescriptionFile(holder: CourseInfoHolder<out Course?>, taskDir: VirtualFile, task: Task): VirtualFile? {
    val descriptionFileName = task.descriptionFormat.fileName
    return createChildFile(holder, taskDir, descriptionFileName, InMemoryTextualContents(task.descriptionText))
  }

  enum class IdeaDirectoryUnpackMode(val insideIdeaDirectory: Boolean, val outsideIdeaDirectory: Boolean) {
    ALL_FILES(true, true),
    ONLY_IDEA_DIRECTORY(false, true),
    ALL_EXCEPT_IDEA_DIRECTORY(true, false)
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  fun unpackAdditionalFiles(holder: CourseInfoHolder<Course>, unpackMode: IdeaDirectoryUnpackMode) {
    val course = holder.course
    for (file in course.additionalFiles) {
      val insideIdeaDirectory = file.name == Project.DIRECTORY_STORE_FOLDER || file.name.startsWith("${Project.DIRECTORY_STORE_FOLDER}/")

      if (insideIdeaDirectory && unpackMode.outsideIdeaDirectory || !insideIdeaDirectory && unpackMode.insideIdeaDirectory) {
        createChildFile(holder, holder.courseDir, file.name, file.contents, file.isEditable)
      }
    }
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  @Deprecated(
    "Use the other createChildFile() method, where you explicitly specify whether text is binary or not",
    ReplaceWith("GeneratorUtils.createChildFile(holder, parentDir, path, fileContent, isEditable)")
  )
  fun createChildFile(project: Project, parentDir: VirtualFile, path: String, text: String, isEditable: Boolean = true): VirtualFile? {
    return createChildFile(project.toCourseInfoHolder(), parentDir, path, InMemoryUndeterminedContents(text), isEditable)
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  fun createChildFile(project: Project, parentDir: VirtualFile, path: String, contents: FileContents): VirtualFile? {
    return createChildFile(project.toCourseInfoHolder(), parentDir, path, contents)
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  fun createTextChildFile(project: Project, parentDir: VirtualFile, path: String, text: String): VirtualFile? {
    return createTextChildFile(project.toCourseInfoHolder(), parentDir, path, text)
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  fun createTextChildFile(
    holder: CourseInfoHolder<out Course?>,
    parentDir: VirtualFile,
    path: String,
    text: String,
    isEditable: Boolean = true
  ): VirtualFile? {
    return createChildFile(holder, parentDir, path, InMemoryTextualContents(text), isEditable)
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  fun createChildFile(
    holder: CourseInfoHolder<out Course?>,
    parentDir: VirtualFile,
    path: String,
    fileContents: FileContents,
    isEditable: Boolean = true
  ): VirtualFile? {
    return runInWriteActionAndWait {
      val file = doCreateChildFile(holder, parentDir, path, fileContents)
      val course = holder.course
      if (course != null && file != null && !isEditable) {
        addNonEditableFileToCourse(course, file)
      }
      file
    }
  }

  @Throws(IOException::class)
  private fun doCreateChildFile(
    holder: CourseInfoHolder<out Course?>,
    parentDir: VirtualFile,
    path: String,
    fileContents: FileContents
  ): VirtualFile? {
    checkIsWriteActionAllowed()

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

    dir ?: return null

    val virtualTaskFile = dir.findOrCreateChildData(parentDir, fileName)

    // If the file is not writable, and we try to set its content, then an AccessDenied exception will pop up.
    // But in some situations (for example, when there is a not-default file system behind),
    // It will be executed without exception despite the file being not writable
    if (!virtualTaskFile.isWritable) error("Attempt to write to read-only file")

    fun writeBinary(bytes: ByteArray) {
      virtualTaskFile.setBinaryContent(bytes)
    }

    fun writeTextual(text: String) {
      val expandedText = EduMacroUtils.expandMacrosForFile(holder, virtualTaskFile, text)
      VfsUtil.saveText(virtualTaskFile, expandedText)
    }

    when (fileContents) {
      is BinaryContents -> writeBinary(fileContents.bytes)
      is TextualContents -> writeTextual(fileContents.text)
      is UndeterminedContents -> if (virtualTaskFile.isToEncodeContent) { // fallback to the legacy way to interpret task file content
        writeBinary(fileContents.bytes)
      }
      else {
        writeTextual(fileContents.text)
      }
    }

    return virtualTaskFile
  }

  @Throws(IOException::class)
  fun addNonEditableFileToCourse(course: Course, virtualTaskFile: VirtualFile) {
    checkIsWriteActionAllowed()
    if (course.isStudy) {
      course.addNonEditableFile(virtualTaskFile.path)
      ReadOnlyAttributeUtil.setReadOnlyAttribute(virtualTaskFile, true)
    }
  }

  @Throws(IOException::class)
  fun removeNonEditableFileFromCourse(course: Course, virtualTaskFile: VirtualFile) {
    ApplicationManager.getApplication().assertWriteAccessAllowed()
    if (course.isStudy) {
      course.removeNonEditableFile(virtualTaskFile.path)
      ReadOnlyAttributeUtil.setReadOnlyAttribute(virtualTaskFile, false)
    }
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  fun <T> runInWriteActionAndWait(action: ThrowableComputable<T, IOException>): T {
    val application = ApplicationManager.getApplication()
    val resultRef = AtomicReference<T>()
    val exceptionRef = AtomicReference<IOException>()
    application.invokeAndWait {
      application.runWriteAction {
        try {
          resultRef.set(action.compute())
        }
        catch (e: IOException) {
          exceptionRef.set(e)
        }
      }
    }
    return if (exceptionRef.get() != null) {
      throw IOException(exceptionRef.get())
    }
    else {
      resultRef.get()
    }
  }

  /**
   * Non unique lesson/task/section names can be received from stepik
   */
  private fun getUniqueValidName(parentDir: VirtualFile, name: String): String {
    val validName = name.convertToValidName()
    var index = 0
    var candidateName = validName
    while (parentDir.findChild(candidateName) != null) {
      index++
      candidateName = "$validName ($index)"
    }
    return candidateName
  }

  fun String.convertToValidName(): String {
    return replace(INVALID_SYMBOLS, " ").trimEnd(*INVALID_TRAILING_SYMBOLS)
  }

  @RequiresBlockingContext
  fun createUniqueDir(
    parentDir: VirtualFile,
    item: StudyItem,
    baseDirName: String = item.name,
    needUpdateItem: Boolean = true
  ): VirtualFile {
    val uniqueDirName = getUniqueValidName(parentDir, baseDirName)
    if (uniqueDirName != baseDirName && needUpdateItem) {
      item.customPresentableName = item.name
      item.name = uniqueDirName
    }

    return runInWriteActionAndWait {
      VfsUtil.createDirectoryIfMissing(parentDir, uniqueDirName)
    }
  }

  fun joinPaths(prefix: String?, suffix: String): String {
    return if (prefix.isNullOrEmpty()) suffix else "$prefix${VfsUtilCore.VFS_SEPARATOR_CHAR}$suffix"
  }

  fun joinPaths(vararg paths: String): String {
    return paths.filter { it.isNotBlank() }.joinToString(VfsUtilCore.VFS_SEPARATOR_CHAR.toString())
  }

  fun getInternalTemplateText(templateName: String, templateVariables: Map<String, Any> = emptyMap()): String =
    FileTemplateManager.getDefaultInstance().getInternalTemplate(templateName).getText(templateVariables)

  fun getJ2eeTemplateText(templateName: String): String =
    FileTemplateManager.getDefaultInstance().getJ2eeTemplate(templateName).text

  @RequiresBlockingContext
  @Throws(IOException::class)
  private fun evaluateExistingTemplate(child: VirtualFile, templateVariables: Map<String, Any>): String {
    val rawContent = VfsUtil.loadText(child)
    return FileTemplateUtil.mergeTemplate(templateVariables, rawContent, false)
  }

  /**
   * Checks if file exists in `courseDir` from [holder] by given [path].
   * If it doesn't exist, creates a new file from internal [templateName] template.
   * Otherwise, substitutes all template variables in file text
   */
  @RequiresBlockingContext
  @Throws(IOException::class)
  fun createFileFromTemplate(
    holder: CourseInfoHolder<out Course?>,
    path: String,
    templateName: String,
    templateVariables: Map<String, Any>
  ) {
    val eduFile = createFromInternalTemplateOrFromDisk(holder.courseDir, path, templateName, templateVariables)
    createChildFile(holder, holder.courseDir, path, eduFile.contents)
  }

  /**
   * Searches for a template in the [courseDir] at the path [templatePath].
   * If the template does not exist, the internal template with the name [internalTemplateName] is used.
   *
   * The template is substituted with [templateVariables], and the result is returned as an [EduFile].
   */
  @RequiresBlockingContext
  @Throws(IOException::class)
  fun createFromInternalTemplateOrFromDisk(
    courseDir: VirtualFile,
    templatePath: String,
    internalTemplateName: String,
    templateVariables: Map<String, Any>
  ): EduFile {
    val file = courseDir.findFileByRelativePath(templatePath)
    val configText = if (file == null) {
      getInternalTemplateText(internalTemplateName, templateVariables)
    }
    else {
      evaluateExistingTemplate(file, templateVariables)
    }
    return EduFile(templatePath, configText)
  }

  /**
   * Removes [module] from [project].
   * It should be used when external build system like Gradle, sbt, etc. creates modules itself
   * and initial base module is unexpected while import
   */
  @RequiresBlockingContext
  fun removeModule(project: Project, module: Module) {
    @Suppress("TestOnlyProblems")
    if (!isUnitTestMode || !project.isLight) {
      project.modifyModules { disposeModule(module) }
    }
  }

  /**
   * Reformat the code so that learners do not see tons of IDE highlighting.
   * Should be used for third-party sources of courses when language style guide is systematically ignored.
   * */
  @RequiresBlockingContext
  fun reformatCodeInAllTaskFiles(project: Project, course: Course) {
    course.visitTasks {
      for ((_, file) in it.taskFiles) {
        val virtualFile = file.getVirtualFile(project) ?: continue
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: continue
        runInEdt {
          WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(psiFile)
          }
        }
      }
    }
  }

  private val GRADLE_INVALID_SYMBOLS = "[ /\\\\:<>\"?*|()]".toRegex()
  private val LEADING_AND_TRAILING_DOTS = "(^[.]+)|([.]+$)".toRegex()

  // Should be the same as `sanitizeName` in `resources/fileTemplates/internal/settings.gradle.ft`
  /**
   * Replaces ' ', '/', '\', ':', '<', '>', '"', '?', '*', '|', '(', ')' symbols with '_' as they are invalid in gradle module names
   * Also removes leading and trailing dots, because gradle project name must not start or end with a '.'
   */
  fun gradleSanitizeName(name: String): String = name.replace(GRADLE_INVALID_SYMBOLS, "_").replace(LEADING_AND_TRAILING_DOTS, "")

  fun getDefaultName(item: StudyItem) = when (item) {
    is Section -> "$SECTION${item.index}"
    is FrameworkLesson -> "${EduNames.FRAMEWORK_LESSON}${item.index}"
    is Lesson -> "$LESSON${item.index}"
    is Task -> "$TASK${item.index}"
    else -> "NonCommonStudyItem${item.index}"
  }
}
