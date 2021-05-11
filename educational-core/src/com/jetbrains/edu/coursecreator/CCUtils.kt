package com.jetbrains.edu.coursecreator

import com.intellij.ide.projectView.ProjectView
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileTooBigException
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.*
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.Function
import com.intellij.util.PathUtil
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showErrorNotification
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikAuthorizer
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.LessonAdditionalInfo
import com.jetbrains.edu.learning.stepik.api.TaskAdditionalInfo
import com.jetbrains.edu.learning.stepik.collectTaskFiles
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.io.IOException
import java.util.*

object CCUtils {
  private val LOG = Logger.getInstance(CCUtils::class.java)

  const val GENERATED_FILES_FOLDER = ".coursecreator"
  const val COURSE_MODE = "Course Creator"
  const val DEFAULT_PLACEHOLDER_TEXT = "type here"

  /**
   * This method decreases index and updates directory names of
   * all tasks/lessons that have higher index than specified object
   *
   * @param dirs         directories that are used to get tasks/lessons
   * @param getStudyItem function that is used to get task/lesson from VirtualFile. This function can return null
   * @param threshold    index is used as threshold
   */
  @JvmStatic
  fun updateHigherElements(
    dirs: Array<VirtualFile>,
    getStudyItem: Function<VirtualFile, out StudyItem?>,
    threshold: Int,
    delta: Int
  ) {
    val itemsToUpdate = dirs
      .mapNotNull { getStudyItem.`fun`(it) }
      .filter { it.index > threshold }
      .sortedWith(Comparator { item1, item2 ->
        // if we delete some dir we should start increasing numbers in dir names from the end
        -delta * EduUtils.INDEX_COMPARATOR.compare(item1, item2)
      })

    for (item in itemsToUpdate) {
      val newIndex = item.index + delta
      item.index = newIndex
    }
  }

  @JvmStatic
  fun getGeneratedFilesFolder(project: Project): VirtualFile? {
    // TODO: come up with a way not to use `Project#getBaseDir`.
    //  Currently, it's supposed that created file path is path in local file system
    //  because it's used for zip archive creation where API uses IO Files instead of virtual files
    @Suppress("DEPRECATION")
    val baseDir = project.baseDir
    val folder = baseDir.findChild(GENERATED_FILES_FOLDER)
    if (folder != null) return folder
    return runWriteAction {
      try {
        val generatedRoot = baseDir.createChildDirectory(this, GENERATED_FILES_FOLDER)
        val contentRootForFile = ProjectRootManager.getInstance(project).fileIndex.getContentRootForFile(generatedRoot)
                                 ?: return@runWriteAction generatedRoot
        val module = ModuleUtilCore.findModuleForFile(baseDir, project) ?: return@runWriteAction generatedRoot
        ModuleRootModificationUtil.updateExcludedFolders(module, contentRootForFile, emptyList(), listOf(generatedRoot.url))
        generatedRoot
      } catch (e: IOException) {
        LOG.info("Failed to create folder for generated files", e)
        null
      }
    }
  }

  @JvmStatic
  fun generateArchiveFolder(project: Project): VirtualFile? {
    val generatedRoot = getGeneratedFilesFolder(project) ?: return null

    val name = "course"
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
    folder?.refresh(false, true)
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
  fun checkIgnoredFiles(project: Project): String? {
    val excludedFiles = loadExcludedFilePaths(project)
    val filesNotFound = excludedFiles.filter { project.courseDir.findFileByRelativePath(it) == null }
    if (filesNotFound.isNotEmpty()) {
      return """|Files listed in the `${EduNames.COURSE_IGNORE}` are not found in the project:
                |
                |${filesNotFound.joinToString()}""".trimMargin()
    }
    return null
  }

  private fun loadExcludedFilePaths(project: Project): List<String> {
    val courseIgnore = project.courseDir.findChild(EduNames.COURSE_IGNORE)
    if (courseIgnore == null || !courseIgnore.exists()) return emptyList()
    return courseIgnore.document.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
  }

  @JvmStatic
  fun isExcluded(file: VirtualFile, project: Project): Boolean {
    val excludedFiles = loadExcludedFilePaths(project)
    val courseRelativePath = VfsUtil.getRelativePath(file, project.courseDir)
    return courseRelativePath in excludedFiles || courseRelativePath == EduNames.COURSE_IGNORE
  }

  @JvmStatic
  fun collectAdditionalFiles(course: Course, project: Project): List<TaskFile> {
    ApplicationManager.getApplication().invokeAndWait { FileDocumentManager.getInstance().saveAllDocuments() }
    val configurator = course.configurator
    val sanitizedName = FileUtil.sanitizeFileName(course.name)
    val archiveName = String.format("%s.zip", if (sanitizedName.startsWith("_")) EduNames.COURSE else sanitizedName)
    val baseDir = project.courseDir

    val excludedFiles = loadExcludedFilePaths(project)

    val additionalTaskFiles = mutableListOf<TaskFile>()
    VfsUtilCore.visitChildrenRecursively(baseDir, object : VirtualFileVisitor<Any>(NO_FOLLOW_SYMLINKS) {
      override fun visitFile(file: VirtualFile): Boolean {
        @Suppress("NAME_SHADOWING")
        val name = file.name
        if (name == archiveName) return false
        val courseRelativePath = VfsUtil.getRelativePath(file, project.courseDir)
        if (courseRelativePath in excludedFiles) return true
        if (file.isDirectory) {
          // All files inside task directory are already handled by `CCVirtualFileListener`
          // so here we don't need to process them again
          return file.getTask(project) == null
        }
        if (file.isTestsFile(project)) return true
        if (configurator != null && configurator.excludeFromArchive(project, file)) return false

        var taskFile = file.getTaskFile(project)
        if (taskFile == null) {
          try {
            val path = VfsUtilCore.getRelativePath(file, baseDir) ?: return true
            taskFile = TaskFile(path, file.loadEncodedContent())
            additionalTaskFiles.add(taskFile)
          }
          catch (e: FileTooBigException) {
            throw HugeBinaryFileException(file.path, file.length, FileUtilRt.LARGE_FOR_CONTENT_LOADING.toLong(), false)
          }
          catch (e: IOException) {
            LOG.error(e)
          }
        }
        return true
      }
    })
    return additionalTaskFiles
  }

  @JvmStatic
  @Suppress("deprecation")
  fun collectAdditionalLessonInfo(lesson: Lesson, project: Project): LessonAdditionalInfo {
    val nonPluginTasks = lesson.taskList.filter { !it.isPluginTaskType }
    val taskInfo = nonPluginTasks.associateBy(Task::getId) {
      TaskAdditionalInfo(it.name, it.customPresentableName, collectTaskFiles(project, it))
    }
    val courseFiles: List<TaskFile> = if (lesson.course is HyperskillCourse) collectAdditionalFiles(lesson.course, project) else listOf()
    return LessonAdditionalInfo(lesson.customPresentableName, taskInfo, courseFiles)
  }

  /**
   * Replaces placeholder texts with [AnswerPlaceholder.getPossibleAnswer]` for each task file in [course].
   * Note, it doesn't affect files in file system
   */
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
    for (item in section.lessons) {
      initializeLessonPlaceholders(project, item)
    }
  }

  private fun initializeLessonPlaceholders(project: Project, lesson: Lesson) {
    for (task in lesson.taskList) {
      initializeTaskPlaceholders(task, project)
    }
  }

  /**
   * Replaces placeholder texts with [AnswerPlaceholder.getPossibleAnswer]` for each task file in [task].
   * Note, it doesn't affect files in file system
   */
  fun initializeTaskPlaceholders(task: Task, project: Project) {
    for ((path, taskFile) in task.taskFiles) {
      invokeAndWaitIfNeeded {
        val file = LightVirtualFile(PathUtil.getFileName(path), PlainTextFileType.INSTANCE, taskFile.text)
        EduDocumentListener.runWithListener(project, taskFile, file) { document ->
          initializeTaskFilePlaceholders(project, taskFile, document)
        }
      }
    }
  }

  private fun initializeTaskFilePlaceholders(project: Project, taskFile: TaskFile, document: Document) {
    taskFile.sortAnswerPlaceholders()
    for (placeholder in taskFile.answerPlaceholders) {
      replaceAnswerPlaceholder(document, placeholder)
    }
    CommandProcessor.getInstance().executeCommand(project, {
      runWriteAction { FileDocumentManager.getInstance().saveDocumentAsIs(document) }
    }, "Create answer document", "Create answer document")
    taskFile.setText(document.text)
  }

  fun replaceAnswerPlaceholder(document: Document, placeholder: AnswerPlaceholder) {
    val offset = placeholder.offset
    placeholder.placeholderText = document.getText(TextRange.create(offset, offset + placeholder.length))
    placeholder.init()

    runUndoTransparentWriteAction {
      document.replaceString(offset, offset + placeholder.length, placeholder.possibleAnswer)
      FileDocumentManager.getInstance().saveDocumentAsIs(document)
    }
  }

  @JvmStatic
  fun wrapIntoSection(project: Project, course: Course, lessonsToWrap: List<Lesson>, @NonNls sectionName: String): Section? {
    Collections.sort(lessonsToWrap, EduUtils.INDEX_COMPARATOR)
    val minIndex = lessonsToWrap[0].index
    val maxIndex = lessonsToWrap[lessonsToWrap.size - 1].index

    val sectionDir = runWriteAction {
      try {
        VfsUtil.createDirectoryIfMissing(project.courseDir, sectionName)
      }
      catch (e: IOException) {
        LOG.error("Failed to create directory for section $sectionName", e)
        null
      }
    } ?: return null

    val section = createSection(lessonsToWrap, sectionName, minIndex)
    section.course = course

    for (i in lessonsToWrap.indices) {
      val lesson = lessonsToWrap[i]
      val lessonDir = lesson.getDir(project.courseDir)
      if (lessonDir != null) {
        moveLesson(lessonDir, sectionDir)
        lesson.index = i + 1
        lesson.section = section
      }
      course.removeLesson(lesson)
    }

    val delta = -lessonsToWrap.size + 1

    updateHigherElements(project.courseDir.children, Function { file -> course.getItem(file.name) }, maxIndex, delta)
    course.addItem(section, section.index - 1)
    synchronizeChanges(project, course, section)
    return section
  }

  private fun synchronizeChanges(project: Project, course: Course, section: Section) {
    YamlFormatSynchronizer.saveItem(section)
    YamlFormatSynchronizer.saveItem(course)
    ProjectView.getInstance(project).refresh()
  }

  private fun createSection(lessonsToWrap: List<Lesson>, sectionName: String, index: Int): Section {
    val section = Section()
    section.index = index
    section.name = sectionName
    section.addLessons(lessonsToWrap)
    return section
  }

  private fun moveLesson(lessonDir: VirtualFile, sectionDir: VirtualFile) {
    ApplicationManager.getApplication().runWriteAction(object : Runnable {
      override fun run() {
        try {
          lessonDir.move(this, sectionDir)
        }
        catch (e1: IOException) {
          LOG.error("Failed to move lesson " + lessonDir.name + " to the new section " + sectionDir.name)
        }

      }
    })
  }

  @JvmStatic
  fun lessonFromDir(course: Course, lessonDir: VirtualFile, project: Project): Lesson? {
    val parentDir = lessonDir.parent
    if (parentDir != null && parentDir.name == project.courseDir.name) {
      return course.getLesson(lessonDir.name)
    }
    else {
      val sectionDir = lessonDir.parent ?: return null
      val section = course.getSection(sectionDir.name) ?: return null
      return section.getLesson(lessonDir.name)
    }
  }

  @JvmStatic
  @JvmOverloads
  fun askToWrapTopLevelLessons(project: Project, course: EduCourse, @Nls yesText: String = EduCoreBundle.message("label.wrap")): Boolean {
    val result = Messages.showYesNoDialog(
      project,
      EduCoreBundle.message("notification.wrap.lessons.into.section.message"),
      EduCoreBundle.message("notification.wrap.lessons.into.section"),
      yesText,
      EduCoreBundle.message("label.cancel"),
      null
    )
    if (result != Messages.YES) {
      return false
    }
    wrapLessonsIntoSections(project, course)
    return true
  }

  private fun wrapLessonsIntoSections(project: Project, course: Course) {
    ApplicationManager.getApplication().invokeAndWait {
      val lessons = course.lessons
      for (lesson in lessons) {
        wrapIntoSection(project, course, listOf(lesson), "Section. " + StringUtil.capitalize(lesson.name))
      }
    }
  }

  @JvmStatic
  fun pushAvailable(parent: ItemContainer, itemToPush: StudyItem, project: Project): Boolean {
    for (item in parent.items) {
      if (item === itemToPush) {
        continue
      }
      if (item.id == 0 && item.index < itemToPush.index) {
        showErrorNotification(project, "Failed to upload",
                              "Previous siblings are not published yet. Use 'Update Course' action")
        return false
      }
      if (item.id != 0 && item.index > itemToPush.index) {
        showErrorNotification(project, "Failed to upload",
                              "Next siblings are affected. Use 'Update Course' action")
        return false
      }
    }
    return true
  }


  // TODO function is needed to be refactored for localization
  @JvmStatic
  fun showLoginNeededNotification(project: Project, platformName: String, failedActionName: String, authAction: () -> Unit) {
    val text = "Log in to $platformName to $failedActionName"
    val notification = Notification(platformName, "Failed to $failedActionName", text, NotificationType.ERROR)
    notification.addAction(object : DumbAwareAction("Log in") {
      override fun actionPerformed(e: AnActionEvent) {
        authAction()
        notification.expire()
      }
    })
    notification.notify(project)
  }

  // TODO function is needed to be refactored after refactoring [showLoginNeededNotification]
  fun checkIfAuthorized(project: Project,
                        platformName: String,
                        failedActionName: String,
                        isLoggedIn: Boolean,
                        authAction: () -> Unit): Boolean {
    val indicator = ProgressManager.getInstance().progressIndicator
    indicator?.checkCanceled()

    if (!isLoggedIn) {
      showLoginNeededNotification(project, platformName, failedActionName) { authAction() }
      return false
    }
    return true
  }

  @JvmStatic
  fun checkIfAuthorizedToStepik(project: Project, failedActionName: String): Boolean {
    return checkIfAuthorized(project, StepikNames.STEPIK, failedActionName,
                             EduSettings.isLoggedIn()) { StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() } }
  }
}
