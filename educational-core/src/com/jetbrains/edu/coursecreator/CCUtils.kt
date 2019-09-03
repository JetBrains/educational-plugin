package com.jetbrains.edu.coursecreator

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.*
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.*
import com.intellij.util.Function
import com.intellij.util.PathUtil
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.showErrorNotification
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.apache.commons.codec.binary.Base64
import java.io.IOException
import java.util.*

object CCUtils {
  private val LOG = Logger.getInstance(CCUtils::class.java)

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
  fun generateFolder(project: Project, name: String): VirtualFile? {
    val generatedRoot = getGeneratedFilesFolder(project) ?: return null

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
  fun collectAdditionalFiles(course: Course, project: Project): List<TaskFile> {
    ApplicationManager.getApplication().invokeAndWait { FileDocumentManager.getInstance().saveAllDocuments() }
    val configurator = course.configurator
    val sanitizedName = FileUtil.sanitizeFileName(course.name)
    val archiveName = String.format("%s.zip", if (sanitizedName.startsWith("_")) EduNames.COURSE else sanitizedName)
    val baseDir = project.courseDir

    val additionalTaskFiles = mutableListOf<TaskFile>()
    VfsUtilCore.visitChildrenRecursively(baseDir, object : VirtualFileVisitor<Any>(NO_FOLLOW_SYMLINKS) {
      override fun visitFile(file: VirtualFile): Boolean {
        @Suppress("NAME_SHADOWING")
        val name = file.name
        if (name == archiveName) return false
        if (file.isDirectory) {
          // All files inside task directory are already handled by `CCVirtualFileListener`
          // so here we don't need to process them again
          return EduUtils.getTask(file, course) == null
        }
        if (EduUtils.isTestsFile(project, file)) return true
        if (configurator != null && configurator.excludeFromArchive(project, file)) return false

        var taskFile = EduUtils.getTaskFile(project, file)
        if (taskFile == null) {
          val path = VfsUtilCore.getRelativePath(file, baseDir) ?: return true
          taskFile = TaskFile(path, loadText(file))
          try {
            additionalTaskFiles.add(taskFile)
          } catch (e: IOException) {
            LOG.error(e)
          }
        }
        return true
      }
    })
    return additionalTaskFiles
  }

  @JvmStatic
  @Throws(IOException::class)
  fun loadText(file: VirtualFile): String {
    return if (EduUtils.isImage(file.name)) {
      Base64.encodeBase64URLSafeString(VfsUtilCore.loadBytes(file))
    } else {
      VfsUtilCore.loadText(file)
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
    project.courseDir.findChild(section.name) ?: return
    for (item in section.lessons) {
      initializeLessonPlaceholders(project, item)
    }
  }

  private fun initializeLessonPlaceholders(project: Project, lesson: Lesson) {
    for (task in lesson.taskList) {
      initializeTaskPlaceholders(task, project)
    }
  }

  fun initializeTaskPlaceholders(task: Task, project: Project) {
    for (entry in task.taskFiles.entries) {
      invokeAndWaitIfNeeded { runWriteAction { initializeTaskFilePlaceholders(project, entry.value) } }
    }
  }

  private fun initializeTaskFilePlaceholders(project: Project, taskFile: TaskFile) {
    val document = taskFile.getDocument(project) ?: return
    taskFile.sortAnswerPlaceholders()
    for (placeholder in taskFile.answerPlaceholders) {
      replaceAnswerPlaceholder(document, placeholder)
    }
    CommandProcessor.getInstance().executeCommand(project, {
      runWriteAction { FileDocumentManager.getInstance().saveDocumentAsIs(document) }
    }, "Create answer document", "Create answer document")
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

  class PathInputValidator @JvmOverloads constructor(
    private val myParentDir: VirtualFile?,
    private val myName: String? = null
  ) : InputValidatorEx {

    private var myErrorText: String? = null

    override fun checkInput(inputString: String): Boolean {
      if (myParentDir == null) {
        myErrorText = "Invalid parent directory"
        return false
      }
      myErrorText = null
      if (!PathUtil.isValidFileName(inputString)) {
        myErrorText = "Invalid name"
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

  @JvmStatic
  fun wrapIntoSection(project: Project, course: Course, lessonsToWrap: List<Lesson>, sectionName: String): Section? {
    Collections.sort(lessonsToWrap, EduUtils.INDEX_COMPARATOR)
    val minIndex = lessonsToWrap[0].index
    val maxIndex = lessonsToWrap[lessonsToWrap.size - 1].index

    val sectionDir = createSectionDir(project, sectionName) ?: return null

    val section = createSection(lessonsToWrap, sectionName, minIndex)
    section.course = course

    for (i in lessonsToWrap.indices) {
      val lesson = lessonsToWrap[i]
      val lessonDir = lesson.getLessonDir(project)
      if (lessonDir != null) {
        moveLesson(lessonDir, sectionDir)
        lesson.index = i + 1
        lesson.section = section
      }
      course.removeLesson(lesson)
    }

    val delta = -lessonsToWrap.size + 1

    updateHigherElements(project.courseDir.children, Function { file ->  course.getItem(file.name) }, maxIndex, delta)
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
  fun createSectionDir(project: Project, sectionName: String): VirtualFile? {
    return ApplicationManager.getApplication().runWriteAction(Computable<VirtualFile> {
      try {
        return@Computable VfsUtil.createDirectoryIfMissing(project.courseDir, sectionName)
      }
      catch (e1: IOException) {
        LOG.error("Failed to create directory for section $sectionName")
      }

      null
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
  fun askToWrapTopLevelLessons(project: Project, course: EduCourse, yesText: String = "Wrap"): Boolean {
    val result = Messages.showYesNoDialog(project,
                                          "Top-level lessons will be wrapped with sections as it's not allowed to have both " +
                                          "top-level lessons and sections",
                                          "Wrap Lessons Into Sections", yesText, "Cancel", null)
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

}
