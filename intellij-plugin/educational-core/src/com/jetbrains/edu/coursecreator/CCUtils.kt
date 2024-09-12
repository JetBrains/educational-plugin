package com.jetbrains.edu.coursecreator

import com.intellij.CommonBundle
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.NlsContexts.Button
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.Function
import com.intellij.util.PathUtil
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.coursecreator.handlers.StudyItemRefactoringHandler
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showLoginNeededNotification
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.io.IOException
import java.util.*

object CCUtils {
  private val LOG = Logger.getInstance(CCUtils::class.java)

  const val GENERATED_FILES_FOLDER = ".coursecreator"
  const val DEFAULT_PLACEHOLDER_TEXT = "type here"
  private const val IS_LOCAL_COURSE: String = "Edu.IsLocalCourse"

  private val INDEX_COMPARATOR = Comparator.comparingInt(StudyItem::index)

  var Project.isLocalCourse: Boolean
    get() = PropertiesComponent.getInstance(this).getBoolean(IS_LOCAL_COURSE)
    set(value) = PropertiesComponent.getInstance(this).setValue(IS_LOCAL_COURSE, value)

  /**
   * This method decreases index and updates directory names of
   * all tasks/lessons that have higher index than specified object
   *
   * @param dirs         directories that are used to get tasks/lessons
   * @param getStudyItem function that is used to get task/lesson from VirtualFile. This function can return null
   * @param threshold    index is used as threshold
   */
  fun updateHigherElements(
    dirs: Array<VirtualFile>,
    getStudyItem: Function<VirtualFile, out StudyItem?>,
    threshold: Int,
    delta: Int
  ) {
    val itemsToUpdate = dirs
      .mapNotNull { getStudyItem.`fun`(it) }
      .filter { it.index > threshold }
      .sortedWith { item1, item2 ->
        // if we delete some dir we should start increasing numbers in dir names from the end
        -delta * INDEX_COMPARATOR.compare(item1, item2)
      }

    for (item in itemsToUpdate) {
      val newIndex = item.index + delta
      item.index = newIndex
    }
  }

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
      }
      catch (e: IOException) {
        LOG.info("Failed to create folder for generated files", e)
        null
      }
    }
  }

  fun saveOpenedDocuments(project: Project) {
    val openDocuments = FileEditorManager.getInstance(project).openFiles.mapNotNull { FileDocumentManager.getInstance().getDocument(it) }
    openDocuments.forEach { FileDocumentManager.getInstance().saveDocument(it) }
  }

  fun isCourseCreator(project: Project): Boolean {
    val course = StudyTaskManager.getInstance(project).course ?: return false
    return CourseMode.EDUCATOR == course.courseMode || CourseMode.EDUCATOR == EduUtilsKt.getCourseModeForNewlyCreatedProject(project)
  }

  fun updateActionGroup(e: AnActionEvent) {
    val presentation = e.presentation
    val project = e.project
    presentation.isEnabledAndVisible = project != null && isCourseCreator(project)
  }

  /**
   * Replaces placeholder texts with [AnswerPlaceholder.possibleAnswer]` for each task file in [course].
   * Note, it doesn't affect files in file system
   */
  fun initializeCCPlaceholders(holder: CourseInfoHolder<Course>) {
    for (item in holder.course.items) {
      when (item) {
        is Section -> initializeSectionPlaceholders(holder, item)
        is Lesson -> initializeLessonPlaceholders(holder, item)
        else -> LOG.warn("Unknown study item type: `${item.javaClass.canonicalName}`")
      }
    }
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
  }

  private fun initializeSectionPlaceholders(holder: CourseInfoHolder<out Course?>, section: Section) {
    for (item in section.lessons) {
      initializeLessonPlaceholders(holder, item)
    }
  }

  private fun initializeLessonPlaceholders(holder: CourseInfoHolder<out Course?>, lesson: Lesson) {
    for (task in lesson.taskList) {
      initializeTaskPlaceholders(holder, task)
    }
  }

  /**
   * Replaces placeholder texts with [AnswerPlaceholder.possibleAnswer]` for each task file in [task].
   * Note, it doesn't affect files in file system
   */
  @Suppress("UnstableApiUsage")
  fun initializeTaskPlaceholders(holder: CourseInfoHolder<out Course?>, task: Task) {
    for ((path, taskFile) in task.taskFiles) {
      if (taskFile.contents is BinaryContents) continue

      invokeAndWaitIfNeeded {
        val file = LightVirtualFile(PathUtil.getFileName(path), PlainTextFileType.INSTANCE, taskFile.text)
        EduDocumentListener.runWithListener(holder, taskFile, file) { document ->
          initializeTaskFilePlaceholders(taskFile, document)
        }
      }
    }
  }

  private fun initializeTaskFilePlaceholders(taskFile: TaskFile, document: Document) {
    taskFile.sortAnswerPlaceholders()
    for (placeholder in taskFile.answerPlaceholders) {
      replaceAnswerPlaceholder(document, placeholder)
    }
    CommandProcessor.getInstance().executeCommand(
      null,
      { runWriteAction { FileDocumentManager.getInstance().saveDocumentAsIs(document) } },
      EduCoreBundle.message("action.create.answer.document"),
      "Edu Actions"
    )
    taskFile.text = document.text
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

  fun wrapIntoSection(project: Project, course: Course, lessonsToWrap: List<Lesson>, @NonNls sectionName: String): Section? {
    Collections.sort(lessonsToWrap, INDEX_COMPARATOR)
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
    section.parent = course

    for (i in lessonsToWrap.indices) {
      val lesson = lessonsToWrap[i]
      val lessonDir = lesson.getDir(project.courseDir)
      if (lessonDir != null) {
        StudyItemRefactoringHandler.processBeforeLessonMovement(project, lesson, sectionDir)
        CCFrameworkLessonManager.getInstance(project).migrateRecords(lesson, sectionDir)
        moveLesson(lessonDir, sectionDir)
        lesson.index = i + 1
        lesson.parent = section
      }
      course.removeLesson(lesson)
    }

    val delta = -lessonsToWrap.size + 1

    updateHigherElements(project.courseDir.children, { file -> course.getItem(file.name) }, maxIndex, delta)
    course.addItem(section.index - 1, section)
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

  fun askToWrapTopLevelLessons(
    project: Project,
    course: EduCourse,
    @Button yesText: String = EduCoreBundle.message("label.wrap")
  ): Boolean {
    val result = Messages.showYesNoDialog(
      project,
      EduCoreBundle.message("notification.wrap.lessons.into.section.message"),
      EduCoreBundle.message("notification.wrap.lessons.into.section"),
      yesText,
      CommonBundle.getCancelButtonText(),
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
      course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
    }
  }

  private fun checkIfAuthorized(
    project: Project,
    failedActionTitle: String,
    isLoggedIn: Boolean,
    authAction: () -> Unit
  ): Boolean {
    val indicator = ProgressManager.getInstance().progressIndicator
    indicator?.checkCanceled()

    if (!isLoggedIn) {
      showLoginNeededNotification(project, failedActionTitle) { authAction() }
      return false
    }
    return true
  }

  fun checkIfAuthorizedToStepik(project: Project, @Nls(capitalization = Nls.Capitalization.Title) failedActionTitle: String): Boolean {
    return checkIfAuthorized(project, failedActionTitle, EduSettings.isLoggedIn()) {
      StepikConnector.getInstance().doAuthorize()
    }
  }

  /**
   * Use for actions with double naming only (e.g. [com.jetbrains.edu.coursecreator.actions.marketplace.MarketplacePushCourse],
   * [com.jetbrains.edu.coursecreator.actions.stepik.hyperskill.PushHyperskillLesson])
   * Concatenates upload and update action texts for action search in find actions
   */
  fun addGluingSlash(updateText: @NlsActions.ActionText String, uploadText: @NlsActions.ActionText String): String {
    return "${updateText}/${uploadText}"
  }
}
