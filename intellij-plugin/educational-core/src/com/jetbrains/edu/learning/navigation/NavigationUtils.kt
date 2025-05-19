package com.jetbrains.edu.learning.navigation

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.WriteIntentReadAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.placeholderDependencies.PlaceholderDependencyManager
import org.jetbrains.annotations.VisibleForTesting
import javax.swing.tree.TreePath

object NavigationUtils {

  fun nextTask(task: Task): Task? {
    if (isUnsolvedHyperskillStage(task) || isLastHyperskillStage(task) || isLastHyperskillProblem(task)) return null

    val currentLesson = task.lesson
    val taskList = currentLesson.taskList
    if (task.index < taskList.size) return taskList[task.index]

    var nextLesson = nextLesson(currentLesson) ?: return null
    var nextLessonTaskList = nextLesson.taskList
    while (nextLessonTaskList.isEmpty()) {
      nextLesson = nextLesson(nextLesson) ?: return null
      nextLessonTaskList = nextLesson.taskList
    }
    return nextLessonTaskList.firstOrNull()
  }

  fun previousTask(task: Task): Task? {
    if (isFirstHyperskillProblem(task)) return null

    val currentLesson = task.lesson
    val prevTaskIndex = task.index - 2
    if (prevTaskIndex >= 0) return currentLesson.taskList[prevTaskIndex]

    var prevLesson = previousLesson(currentLesson) ?: return null
    //getting last task in previous lesson
    var prevLessonTaskList = prevLesson.taskList
    while (prevLessonTaskList.isEmpty()) {
      prevLesson = previousLesson(prevLesson) ?: return null
      prevLessonTaskList = prevLesson.taskList
    }
    return prevLessonTaskList[prevLessonTaskList.size - 1]
  }

  fun isUnsolvedHyperskillStage(task: Task): Boolean {
    val course = task.course as? HyperskillCourse ?: return false
    if (task.lesson != course.getProjectLesson() || task.status == CheckStatus.Solved) return false
    val stage = course.stages.getOrNull(task.index - 1) ?: return false
    return !stage.isCompleted
  }

  private fun isLastHyperskillStage(task: Task): Boolean {
    val course = task.course as? HyperskillCourse ?: return false
    return task.lesson == course.getProjectLesson() && task.index == course.stages.size
  }

  private fun isFirstHyperskillProblem(task: Task): Boolean {
    val course = task.course as? HyperskillCourse ?: return false
    return task.lesson != course.getProjectLesson() && task.index == 1
  }

  fun isLastHyperskillProblem(task: Task): Boolean {
    val course = task.course as? HyperskillCourse ?: return false
    val lesson = task.lesson
    return lesson != course.getProjectLesson() && task.index == lesson.items.size
  }

  fun nextLesson(lesson: Lesson): Lesson? {
    val container = lesson.container
    val siblings = container.items
    val nextLessonIndex = lesson.index
    if (nextLessonIndex < siblings.size) {
      return nextLesson(siblings, nextLessonIndex)
    }
    if (container is Section) {
      val items = lesson.course.items
      val nextIndex = container.index
      return nextLesson(items, nextIndex)
    }
    return null
  }

  private fun nextLesson(siblings : List<StudyItem>, nextIndex : Int) : Lesson? {
    if (nextIndex >= siblings.size) {
      return null
    }
    val item = siblings[nextIndex]
    if (item is Lesson) {
      return item
    }
    else if (item is Section) {
      val sectionLessons = item.lessons
      if (sectionLessons.isNotEmpty()) {
        return sectionLessons[0]
      }
    }
    return null
  }

  private fun previousLesson(lesson: Lesson): Lesson? {
    val container = lesson.container
    val siblings = container.items
    val prevLessonIndex = lesson.index - 2
    if (prevLessonIndex >= 0) {
      return previousLesson(siblings, prevLessonIndex)
    }
    if (container is Section) {
      val items = lesson.course.items
      val previousItemIndex = container.index - 2
      return previousLesson(items, previousItemIndex)
    }
    return null
  }

  private fun previousLesson(siblings : List<StudyItem>, prevIndex : Int) : Lesson? {
    if (prevIndex < 0) {
      return null
    }
    val item = siblings[prevIndex]
    if (item is Lesson) {
      return item
    }
    else if (item is Section) {
      val sectionLessons = item.lessons
      if (sectionLessons.isNotEmpty()) {
        return sectionLessons[sectionLessons.size - 1]
      }
    }
    return null
  }

  fun navigateToAnswerPlaceholder(editor: Editor, answerPlaceholder: AnswerPlaceholder) {
    if (editor.isDisposed) return
    val offsets = getPlaceholderOffsets(answerPlaceholder)
    editor.caretModel.moveToOffset(offsets.first)
    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
    editor.selectionModel.setSelection(offsets.first, offsets.second)
  }

  fun navigateToFirstAnswerPlaceholder(editor: Editor, taskFile: TaskFile) {
    val visiblePlaceholders = taskFile.answerPlaceholders.filter { it.isCurrentlyVisible }
    val firstAnswerPlaceholder = visiblePlaceholders.firstOrNull() ?: return
    navigateToAnswerPlaceholder(editor, firstAnswerPlaceholder)
  }

  @VisibleForTesting
  fun getFirstTask(course: Course): Task? {
    WriteIntentReadAction.run {
      LocalFileSystem.getInstance().refresh(false)
    }
    val firstItem = course.items.firstOrNull() ?: return null
    val firstLesson = if (firstItem is Section) firstItem.lessons.firstOrNull() else firstItem as Lesson
    if (firstLesson != null) return firstLesson.taskList.firstOrNull()
    return null
  }

  fun openFirstTask(course: Course, project: Project) {
    val firstTask = getFirstTask(course) ?: return
    navigateToTask(project, firstTask)
  }

  private fun getFirstTaskFile(taskDir: VirtualFile, task: Task): VirtualFile? {
    val taskFiles = task.taskFiles.values
    val firstVisibleTaskFile = taskFiles.firstOrNull { it.isVisible } ?: return null
    return firstVisibleTaskFile.findTaskFileInDir(taskDir)
  }

  fun navigateToTask(project: Project, sectionName: String?, lessonName: String, taskName: String) {
    val course = StudyTaskManager.getInstance(project).course ?: return
    val lesson = course.getLesson(sectionName, lessonName) ?: return
    val task = lesson.getTask(taskName) ?: return
    project.invokeLater { navigateToTask(project, task) }
  }

  fun navigateToTask(
    project: Project,
    task: Task,
    fromTask: Task? = null,
    showDialogIfConflict: Boolean = true,
    closeOpenedFiles: Boolean = true,
    fileToActivate: VirtualFile? = null
  ) {
    runInEdt {
      WriteIntentReadAction.run {
        navigateToTaskInternal(project, task, fromTask, showDialogIfConflict, closeOpenedFiles, fileToActivate)
      }
      TaskNavigationExtension.EP.forEachExtensionSafe {
        it.onTaskNavigation(project, task, fromTask)
      }
    }
  }

  @RequiresEdt
  private fun navigateToTaskInternal(
    project: Project,
    task: Task,
    fromTask: Task?,
    showDialogIfConflict: Boolean,
    closeOpenedFiles: Boolean,
    fileToActivate: VirtualFile?
  ) {
    if (closeOpenedFiles) {
      for (file in FileEditorManager.getInstance(project).openFiles) {
        FileEditorManager.getInstance(project).closeFile(file)
      }
    }
    if (CCUtils.isCourseCreator(project)) {
      openCCTaskFiles(project, task)
      return
    }
    val taskFiles = task.taskFiles

    val lesson = task.lesson

    // We should save student answers and apply diffs only in student mode
    if (lesson is FrameworkLesson && lesson.course.isStudy && fromTask != null && fromTask.lesson == lesson) {
      fromTask.saveStudentAnswersIfNeeded(project)
      prepareFilesForTargetTask(project, lesson, fromTask, task, showDialogIfConflict)
      project.course?.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
    }

    val taskDir = task.getDir(project.courseDir) ?: return

    if (taskFiles.isEmpty()) {
      val selectingDir = task.findSourceDir(taskDir) ?: taskDir
      ProjectView.getInstance(project).select(selectingDir, selectingDir, false)
      return
    }

    // We need update dependencies before file opening to find out which placeholders are visible
    PlaceholderDependencyManager.updateDependentPlaceholders(project, task)

    @Suppress("NAME_SHADOWING")
    var fileToActivate : VirtualFile? = fileToActivate

    for ((_, taskFile) in taskFiles) {
      if (taskFile.answerPlaceholders.isEmpty()) continue
      // We want to open task file only if it has `new` placeholder(s).
      // Currently, we consider that `new` placeholder is a visible placeholder,
      // i.e. placeholder without dependency or with visible dependency.
      if (!taskFile.isVisible || taskFile.answerPlaceholders.all { !it.isCurrentlyVisible }) continue
      val virtualFile = taskFile.findTaskFileInDir(taskDir) ?: continue
      FileEditorManager.getInstance(project).openFile(virtualFile, true)
      if (fileToActivate == null) {
        fileToActivate = virtualFile
      }
    }
    if (fileToActivate == null) {
      fileToActivate = getFirstTaskFile(taskDir, task)
    }

    ProjectView.getInstance(project).refresh()
    if (fileToActivate != null) {
      updateProjectView(project, fileToActivate)
    }

    selectFirstAnswerPlaceholder(project)
    ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)?.hide(null)

    if (lesson is FrameworkLesson && lesson.course.isStudy) {
      runInEdt {
        runWriteAction {
          setHighlightLevelForFilesInTask(task, project)
        }
      }
    }
  }

  fun setHighlightLevelForFilesInTask(task: Task, project: Project) {
    checkIsWriteActionAllowed()
    for (taskFile in task.taskFiles.values) {
      val virtualFile = taskFile.getVirtualFile(project)
      virtualFile?.setHighlightLevelInsideWriteAction(project, taskFile.errorHighlightLevel)
    }
  }

  @RequiresEdt
  private fun openCCTaskFiles(project: Project, task: Task) {
    val taskDir = task.getDir(project.courseDir) ?: return
    val descriptionFile = task.getDescriptionFile(project)
    descriptionFile?.let { FileEditorManager.getInstance(project).openFile(it, false) }

    task.getAllTestVFiles(project).forEach { testFile ->
      FileEditorManager.getInstance(project).openFile(testFile, false)
    }
    val firstTaskFile = getFirstTaskFile(taskDir, task)
    ProjectView.getInstance(project).refresh()
    firstTaskFile?.let { updateProjectView(project, it) }
  }

  private fun selectFirstAnswerPlaceholder(project: Project) {
    val eduState = project.eduState ?: return
    val (_, _, editor, taskFile, _, _) = eduState

    IdeFocusManager.getInstance(project).requestFocus(editor.contentComponent, true)
    if (!taskFile.isValid(editor.document.text)) {
      return
    }

    val placeholder = taskFile.answerPlaceholders.firstOrNull { it.isCurrentlyVisible } ?: return
    val offsets = getPlaceholderOffsets(placeholder)

    with(editor) {
      selectionModel.setSelection(offsets.first, offsets.second)
      caretModel.moveToOffset(offsets.first)
      scrollingModel.scrollToCaret(ScrollType.CENTER)
    }
  }

  fun getPlaceholderOffsets(answerPlaceholder: AnswerPlaceholder): Pair<Int, Int> {
    return Pair.create(answerPlaceholder.offset, answerPlaceholder.endOffset)
  }

  @RequiresEdt
  @RequiresBlockingContext
  fun prepareFilesForTargetTask(
    project: Project,
    frameworkLesson: FrameworkLesson,
    currentTask: Task, targetTask: Task,
    showDialogIfConflict: Boolean
  ) {
    val dir = currentTask.getDir(project.courseDir) ?: return
    setReadOnlyFlagToNonEditableFiles(project, currentTask, false)

    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)
    @Suppress("NAME_SHADOWING")
    var currentTask = currentTask
    while (currentTask.index != targetTask.index) {
      if (currentTask.index < targetTask.index) {
        frameworkLessonManager.prepareNextTask(frameworkLesson, dir, showDialogIfConflict)
      }
      else {
        frameworkLessonManager.preparePrevTask(frameworkLesson, dir, showDialogIfConflict)
      }
      currentTask = frameworkLesson.currentTask() ?: break
    }

    runWriteAction {
      FileDocumentManager.getInstance().saveAllDocuments()
    }
    setReadOnlyFlagToNonEditableFiles(project, currentTask, true)
  }

  @RequiresEdt
  private fun updateProjectView(project: Project, fileToActivate: VirtualFile) {
    FileEditorManager.getInstance(project).openFile(fileToActivate, true)
    val viewPane = ProjectView.getInstance(project).currentProjectViewPane ?: return
    val tree = viewPane.tree
    ProjectView.getInstance(project).selectCB(fileToActivate, fileToActivate, false).doWhenDone {
      val paths = TreeUtil.collectExpandedPaths(tree)
      val toCollapse = ArrayList<TreePath>()
      val selectedPath = tree.selectionPath
      for (treePath in paths) {
        if (treePath.isDescendant(selectedPath)) continue
        if (toCollapse.isEmpty()) {
          toCollapse.add(treePath)
          continue
        }
        for (i in toCollapse.indices) {
          val path = toCollapse[i]
          if (treePath.isDescendant(path)) {
            toCollapse[i] = treePath
          } else {
            if (!path.isDescendant(treePath)) {
              toCollapse.add(treePath)
            }
          }
        }
      }
      for (path in toCollapse) {
        tree.collapsePath(path)
        tree.fireTreeCollapsed(path)
      }
    }
  }

  private fun setReadOnlyFlagToNonEditableFiles(project: Project, task: Task, readOnlyFlag: Boolean) {
    runWriteAction {
      for (taskFile in task.taskFiles.values.filter { !it.isEditable }) {
        val virtualTaskFile = taskFile.getVirtualFile(project) ?: error("Cannot get a virtual file")
        if (readOnlyFlag) {
          GeneratorUtils.addNonEditableFileToCourse(task.course, virtualTaskFile)
        } else {
          GeneratorUtils.removeNonEditableFileFromCourse(task.course, virtualTaskFile)
        }
      }
    }
  }
}
