package com.jetbrains.edu.learning.navigation

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.findSourceDir
import com.jetbrains.edu.learning.courseFormat.ext.saveStudentAnswersIfNeeded
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.placeholderDependencies.PlaceholderDependencyManager
import com.jetbrains.edu.learning.statistics.EduUsagesCollector
import java.util.*
import javax.swing.tree.TreePath

object NavigationUtils {

  @JvmStatic
  fun nextTask(task: Task): Task? {
    val currentLesson = task.lesson
    val taskList = currentLesson.getTaskList()
    if (task.index < taskList.size) return taskList[task.index]

    var nextLesson = nextLesson(currentLesson) ?: return null
    var nextLessonTaskList = nextLesson.getTaskList()
    while (nextLessonTaskList.isEmpty()) {
      nextLesson = nextLesson(nextLesson) ?: return null
      nextLessonTaskList = nextLesson.getTaskList()
    }
    return EduUtils.getFirst(nextLessonTaskList)
  }

  @JvmStatic
  fun previousTask(task: Task): Task? {
    val currentLesson = task.lesson
    val prevTaskIndex = task.index - 2
    if (prevTaskIndex >= 0) return currentLesson.getTaskList()[prevTaskIndex]

    var prevLesson = previousLesson(currentLesson) ?: return null
    //getting last task in previous lesson
    var prevLessonTaskList = prevLesson.getTaskList()
    while (prevLessonTaskList.isEmpty()) {
      prevLesson = previousLesson(prevLesson) ?: return null
      prevLessonTaskList = prevLesson.getTaskList()
    }
    return prevLessonTaskList[prevLessonTaskList.size - 1]
  }

  private fun nextLesson(lesson: Lesson): Lesson? {
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
      if (!sectionLessons.isEmpty()) {
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
      if (!sectionLessons.isEmpty()) {
        return sectionLessons[sectionLessons.size - 1]
      }
    }
    return null
  }

  @JvmStatic
  fun navigateToFirstFailedAnswerPlaceholder(editor: Editor, taskFile: TaskFile) {
    editor.project ?: return
    for (answerPlaceholder in taskFile.answerPlaceholders) {
      if (answerPlaceholder.status != CheckStatus.Failed || !answerPlaceholder.isVisible) continue
      navigateToAnswerPlaceholder(editor, answerPlaceholder)
      break
    }
  }

  @JvmStatic
  fun navigateToAnswerPlaceholder(editor: Editor, answerPlaceholder: AnswerPlaceholder) {
    if (editor.isDisposed) return
    val offsets = EduUtils.getPlaceholderOffsets(answerPlaceholder)
    editor.caretModel.moveToOffset(offsets.first)
    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
    editor.selectionModel.setSelection(offsets.first, offsets.second)
  }

  @JvmStatic
  fun navigateToFirstAnswerPlaceholder(editor: Editor, taskFile: TaskFile) {
    val visiblePlaceholders = taskFile.answerPlaceholders.filter { it.isVisible }
    val firstAnswerPlaceholder = EduUtils.getFirst(visiblePlaceholders) ?: return
    navigateToAnswerPlaceholder(editor, firstAnswerPlaceholder)
  }

  private fun getFirstTaskFile(taskDir: VirtualFile, taskFiles: MutableCollection<TaskFile>): VirtualFile? {
    return taskFiles.map { EduUtils.findTaskFileInDir(it, taskDir) }.firstOrNull()
  }

  @JvmStatic
  fun navigateToTask(project: Project, sectionName: String?, lessonName: String, taskName: String) {
    val course = StudyTaskManager.getInstance(project).course ?: return
    val lesson = course.getLesson(sectionName, lessonName) ?: return
    val task = lesson.getTask(taskName) ?: return
    ApplicationManager.getApplication().invokeLater { navigateToTask(project, task) }
  }

  @JvmOverloads
  @JvmStatic
  fun navigateToTask(project: Project, task: Task, fromTask: Task? = null) {
    for (file in FileEditorManager.getInstance(project).openFiles) {
      FileEditorManager.getInstance(project).closeFile(file)
    }
    val taskFiles = task.getTaskFiles()

    val lesson = task.lesson

    // We should save student answers and apply diffs only in student mode
    if (lesson is FrameworkLesson && lesson.course.isStudy && fromTask != null && fromTask.lesson == lesson) {
      fromTask.saveStudentAnswersIfNeeded(project)
      prepareFilesForTargetTask(project, lesson, fromTask, task)
    }

    val taskDir = task.getTaskDir(project) ?: return

    if (taskFiles.isEmpty()) {
      val selectingDir = task.findSourceDir(taskDir) ?: taskDir
      ProjectView.getInstance(project).select(selectingDir, selectingDir, false)
      return
    }

    // We need update dependencies before file opening to find out which placeholders are visible
    PlaceholderDependencyManager.updateDependentPlaceholders(project, task)

    var fileToActivate : VirtualFile? = null

    for ((_, taskFile) in taskFiles) {
      if (taskFile.answerPlaceholders.isEmpty()) continue
      // We want to open task file only if it has `new` placeholder(s).
      // Currently, we consider that `new` placeholder is a visible placeholder,
      // i.e. placeholder without dependency or with visible dependency.
      if (taskFile.answerPlaceholders.all { !it.isVisible }) continue
      val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
      FileEditorManager.getInstance(project).openFile(virtualFile, true)
      fileToActivate = virtualFile
    }
    if (fileToActivate == null) {
      fileToActivate = getFirstTaskFile(taskDir, taskFiles.values)
    }

    EduUsagesCollector.taskNavigation()
    ProjectView.getInstance(project).refresh()
    if (fileToActivate != null) {
      updateProjectView(project, fileToActivate)
    }

    EduUtils.selectFirstAnswerPlaceholder(EduUtils.getSelectedEduEditor(project), project)
    ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)?.hide(null)
  }

  private fun prepareFilesForTargetTask(project: Project, frameworkLesson: FrameworkLesson, currentTask: Task, targetTask: Task) {
    val dir = currentTask.getTaskDir(project) ?: return

    @Suppress("NAME_SHADOWING")
    var currentTask = currentTask
    while (currentTask.index != targetTask.index) {
      if (currentTask.index < targetTask.index) {
        frameworkLesson.prepareNextTask(project, dir)
      } else {
        frameworkLesson.preparePrevTask(project, dir)
      }
      currentTask = frameworkLesson.currentTask()
    }
  }

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
}

