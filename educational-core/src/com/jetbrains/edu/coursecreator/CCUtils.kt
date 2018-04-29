package com.jetbrains.edu.coursecreator

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.util.Function
import com.intellij.util.PathUtil
import com.intellij.util.ThrowableConsumer
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.ext.testDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.apache.commons.codec.binary.Base64
import java.io.IOException

object CCUtils {
  private val LOG = Logger.getInstance(CCUtils::class.java)

  const val ANSWER_EXTENSION_DOTTED = ".answer."
  const val GENERATED_FILES_FOLDER = ".coursecreator"
  const val COURSE_MODE = "Course Creator"

  /**
   * This method decreases index and updates directory names of
   * all tasks/lessons that have higher index than specified object
   *
   * @param dirs         directories that are used to get tasks/lessons
   * @param getStudyItem function that is used to get task/lesson from VirtualFile. This function can return null
   * @param threshold    index is used as threshold
   */
  @JvmStatic
  fun updateHigherElements(dirs: Array<VirtualFile>,
                           getStudyItem: Function<VirtualFile, out StudyItem>,
                           threshold: Int,
                           delta: Int) {
    val itemsToUpdate = dirs.filterTo(mutableListOf()) { dir ->
      val item = getStudyItem.`fun`(dir) ?: return@filterTo false
      item.index > threshold
    }.sortedWith(Comparator { o1, o2 ->
      val item1 = getStudyItem.`fun`(o1)
      val item2 = getStudyItem.`fun`(o2)
      //if we delete some dir we should start increasing numbers in dir names from the end
      -delta * EduUtils.INDEX_COMPARATOR.compare(item1, item2)
    })

    for (dir in itemsToUpdate) {
      val item = getStudyItem.`fun`(dir)
      val newIndex = item.index + delta
      item.index = newIndex
    }
  }

  @JvmStatic
  fun getGeneratedFilesFolder(project: Project, module: Module): VirtualFile? {
    val baseDir = project.baseDir
    val folder = baseDir.findChild(GENERATED_FILES_FOLDER)
    if (folder != null) return folder
    return runWriteAction {
      try {
        val generatedRoot = baseDir.createChildDirectory(this, GENERATED_FILES_FOLDER)
        val contentRootForFile = ProjectRootManager.getInstance(module.project).fileIndex.getContentRootForFile(generatedRoot)
                                 ?: return@runWriteAction null
        ModuleRootModificationUtil.updateExcludedFolders(module, contentRootForFile, emptyList(), listOf(generatedRoot.url))
        generatedRoot
      } catch (e: IOException) {
        LOG.info("Failed to create folder for generated files", e)
        null
      }
    }
  }

  @JvmStatic
  fun generateFolder(project: Project, module: Module, name: String): VirtualFile? {
    val generatedRoot = getGeneratedFilesFolder(project, module) ?: return null

    var folder = generatedRoot.findChild(name)
    //need to delete old folder
    runWriteAction {
      try {
        folder?.delete(CCUtils::class.java)
        folder = generatedRoot.createChildDirectory(null, name)
      } catch (e: IOException) {
        LOG.info("Failed to generate folder $name", e)
      }
    }
    return folder
  }

  @JvmStatic
  fun isCourseCreator(project: Project): Boolean {
    val course = StudyTaskManager.getInstance(project).course ?: return false
    return COURSE_MODE == course.courseMode || COURSE_MODE == EduUtils.getCourseModeForNewlyCreatedProject(project)
  }

  @JvmStatic
  fun updateActionGroup(e: AnActionEvent) {
    val presentation = e.presentation
    val project = e.project
    presentation.isEnabledAndVisible = project != null && isCourseCreator(project)
  }

  @JvmStatic
  fun createAdditionalLesson(course: Course, project: Project,
                             name: String): Lesson? {
    val baseDir = project.baseDir
    val configurator = EduConfiguratorManager.forLanguage(course.languageById!!)

    val lesson = Lesson()
    lesson.name = name
    lesson.course = course
    val task = EduTask()
    task.lesson = lesson
    task.name = name
    task.index = 1

    val sanitizedName = FileUtil.sanitizeFileName(course.name)
    val archiveName = String.format("%s.zip", if (sanitizedName.startsWith("_")) EduNames.COURSE else sanitizedName)

    val utilDir = baseDir.findChild(EduNames.UTIL)
    val sourceDirName = course.sourceDir
    val testDirName = course.testDir
    val utilSourceDir: VirtualFile?
    val utilTestDir: VirtualFile?
    if (utilDir != null && sourceDirName != null && testDirName != null) {
      utilSourceDir = utilDir.findChild(sourceDirName)
      utilTestDir = utilDir.findChild(testDirName)
    } else {
      utilSourceDir = null
      utilTestDir = null
    }

    VfsUtilCore.visitChildrenRecursively(baseDir, object : VirtualFileVisitor<Any>(VirtualFileVisitor.NO_FOLLOW_SYMLINKS) {
      override fun visitFile(file: VirtualFile): Boolean {
        @Suppress("NAME_SHADOWING")
        val name = file.name
        if (name == EduNames.COURSE_META_FILE || name == EduNames.HINTS || name.startsWith(".")) return false
        if (name == archiveName) return false
        if (GENERATED_FILES_FOLDER == name || Project.DIRECTORY_STORE_FOLDER == name) return false
        if (file.isDirectory) return true
        if (EduUtils.isTaskDescriptionFile(name) || EduUtils.isTestsFile(project, file)) return true
        if (name.contains(".iml") || configurator != null && configurator.excludeFromArchive(file.path)) return false

        val taskFile = EduUtils.getTaskFile(project, file)
        if (taskFile == null) {
          if (utilSourceDir != null && VfsUtilCore.isAncestor(utilSourceDir, file, true)) {
            addTaskFile(task, baseDir, file)
          } else if (utilTestDir != null && VfsUtilCore.isAncestor(utilTestDir, file, true)) {
            addTestFile(task, baseDir, file)
          } else {
            addAdditionalFile(task, baseDir, file)
          }
        }
        return true
      }
    })
    if (taskIsEmpty(task)) return null
    lesson.addTask(task)
    lesson.index = course.items.size + 1
    return lesson
  }

  private fun taskIsEmpty(task: Task): Boolean = task.getTaskFiles().isEmpty() &&
                                                 task.testsText.isEmpty() &&
                                                 task.additionalFiles.isEmpty()

  private fun addTaskFile(task: Task, baseDir: VirtualFile, file: VirtualFile) {
    addToTask(baseDir, file, ThrowableConsumer { path ->
      val utilTaskFile = TaskFile()
      utilTaskFile.name = path
      utilTaskFile.text = VfsUtilCore.loadText(file)
      utilTaskFile.task = task
      task.addTaskFile(utilTaskFile)
    })
  }

  private fun addTestFile(task: Task, baseDir: VirtualFile, file: VirtualFile) {
    addToTask(baseDir, file, ThrowableConsumer { path -> task.addTestsTexts(path, VfsUtilCore.loadText(file)) })
  }

  private fun addAdditionalFile(task: Task, baseDir: VirtualFile, file: VirtualFile) {
    addToTask(baseDir, file, ThrowableConsumer { path ->
      val text: String = if (EduUtils.isImage(file.name)) {
        Base64.encodeBase64URLSafeString(VfsUtilCore.loadBytes(file))
      } else {
        VfsUtilCore.loadText(file)
      }
      task.addAdditionalFile(path, text)
    })
  }

  private fun addToTask(baseDir: VirtualFile, file: VirtualFile, action: ThrowableConsumer<String, IOException>) {
    val path = VfsUtilCore.getRelativePath(file, baseDir) ?: return
    try {
      action.consume(path)
    } catch (e: IOException) {
      LOG.error(e)
    }
  }

  @JvmStatic
  fun initializeCCPlaceholders(project: Project, course: Course) {
    for (item in course.items) {
      when (item) {
        is Section -> initializeSectionPlaceholders(project, item)
        is Lesson -> initializeLessonPlaceholders(project, item)
        else -> LOG.warn("Unknown study item type: `${item.javaClass.canonicalName}`")
      }
    }
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
  }

  private fun initializeSectionPlaceholders(project: Project, section: Section) {
    EduUtils.getCourseDir(project)?.findChild(section.name) ?: return
    for (item in section.lessons) {
      initializeLessonPlaceholders(project, item)
    }
  }

  private fun initializeLessonPlaceholders(project: Project, lesson: Lesson) {
    val lessonDir = lesson.getLessonDir(project) ?: return
    val application = ApplicationManager.getApplication()

    for (task in lesson.getTaskList()) {
      val taskDir = lessonDir.findChild(task.name)
      if (taskDir == null) continue
      for (entry in task.getTaskFiles().entries) {
        application.invokeAndWait { application.runWriteAction { initializeTaskFilePlaceholders(project, taskDir, entry.value) } }
      }
    }
  }

  private fun initializeTaskFilePlaceholders(project: Project, userFileDir: VirtualFile, taskFile: TaskFile) {
    val file = EduUtils.findTaskFileInDir(taskFile, userFileDir)
    if (file == null) {
      LOG.warn("Failed to find file $file")
      return
    }
    val document = FileDocumentManager.getInstance().getDocument(file) ?: return
    val listener = EduDocumentTransformListener(project, taskFile)
    document.addDocumentListener(listener)
    taskFile.sortAnswerPlaceholders()
    taskFile.isTrackLengths = false

    try {
      for (placeholder in taskFile.answerPlaceholders) {
        replaceAnswerPlaceholder(document, placeholder)
        placeholder.useLength = false
      }

      CommandProcessor.getInstance().executeCommand(project, {
        runWriteAction { FileDocumentManager.getInstance().saveDocumentAsIs(document) }
      }, "Create answer document", "Create answer document")
    } finally {
      document.removeDocumentListener(listener)
      taskFile.isTrackLengths = true
    }
  }

  private fun replaceAnswerPlaceholder(document: Document, placeholder: AnswerPlaceholder) {
    val offset = placeholder.offset
    val text = document.getText(TextRange.create(offset, offset + placeholder.length))
    placeholder.placeholderText = text
    placeholder.init()
    val replacementText = placeholder.possibleAnswer

    runUndoTransparentWriteAction {
      document.replaceString(offset, offset + placeholder.length, replacementText)
      FileDocumentManager.getInstance().saveDocumentAsIs(document)
    }
  }

  class PathInputValidator @JvmOverloads constructor(
    private val myParentDir: VirtualFile?,
    private val myName: String? = null
  ) : InputValidatorEx {

    private var myErrorText: String? = null

    override fun checkInput(inputString: String): Boolean {
      if (myParentDir == null) {
        myErrorText = "invalid parent directory"
        return false
      }
      myErrorText = null
      if (!PathUtil.isValidFileName(inputString)) {
        myErrorText = "invalid name"
        return false
      }
      if (myParentDir.findChild(inputString) != null && inputString != myName) {
        myErrorText = String.format("%s already contains directory named %s", myParentDir.name, inputString)
      }
      return myErrorText == null
    }

    override fun canClose(inputString: String): Boolean = checkInput(inputString)

    override fun getErrorText(inputString: String): String? = myErrorText
  }
}
