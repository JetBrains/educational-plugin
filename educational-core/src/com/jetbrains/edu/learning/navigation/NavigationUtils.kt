package com.jetbrains.edu.learning.navigation

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
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
    val lessons = lesson.course.lessons
    val nextLessonIndex = lesson.index
    return lessons.getOrNull(nextLessonIndex)
  }

  private fun previousLesson(lesson: Lesson): Lesson? {
    val prevLessonIndex = lesson.index - 2
    return lesson.course.lessons.getOrNull(prevLessonIndex)
  }

  @JvmStatic
  fun navigateToFirstFailedAnswerPlaceholder(editor: Editor, taskFile: TaskFile) {
    editor.project ?: return
    for (answerPlaceholder in taskFile.activePlaceholders) {
      if (answerPlaceholder.status != CheckStatus.Failed) continue
      navigateToAnswerPlaceholder(editor, answerPlaceholder)
      break
    }
  }

  @JvmStatic
  fun navigateToAnswerPlaceholder(editor: Editor, answerPlaceholder: AnswerPlaceholder) {
    if (editor.isDisposed) return
    val offsets = EduUtils.getPlaceholderOffsets(answerPlaceholder, editor.document)
    editor.caretModel.moveToOffset(offsets.first)
    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
    editor.selectionModel.setSelection(offsets.first, offsets.second)
  }

  @JvmStatic
  fun navigateToFirstAnswerPlaceholder(editor: Editor, taskFile: TaskFile) {
    if (!taskFile.activePlaceholders.isEmpty()) {
      val firstAnswerPlaceholder = EduUtils.getFirst(taskFile.activePlaceholders) ?: return
      navigateToAnswerPlaceholder(editor, firstAnswerPlaceholder)
    }
  }

  private fun getFirstTaskFile(taskDir: VirtualFile, project: Project): VirtualFile? {
    return taskDir.children.firstOrNull { EduUtils.getTaskFile(project, it) != null }
  }

  @JvmStatic
  fun navigateToTask(project: Project, lessonName: String, taskName: String) {
    val course = StudyTaskManager.getInstance(project).course ?: return
    val lesson = course.getLesson(lessonName) ?: return
    val task = lesson.getTask(taskName) ?: return
    ApplicationManager.getApplication().invokeLater { navigateToTask(project, task) }
  }

  @JvmStatic
  fun navigateToTask(project: Project, task: Task) {
    for (file in FileEditorManager.getInstance(project).openFiles) {
      FileEditorManager.getInstance(project).closeFile(file)
    }
    val taskFiles = task.getTaskFiles()
    var taskDir = task.getTaskDir(project) ?: return
    val sourceDir = task.sourceDir
    if (StringUtil.isNotEmpty(sourceDir)) {
      val srcDir = taskDir.findChild(sourceDir!!)
      if (srcDir != null) {
        taskDir = srcDir
      }
    }
    if (taskFiles.isEmpty()) {
      ProjectView.getInstance(project).select(taskDir, taskDir, false)
      return
    }
    var fileToActivate = getFirstTaskFile(taskDir, project)
    for ((_, taskFile) in taskFiles) {
      if (taskFile.activePlaceholders.isEmpty()) continue
      val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
      FileEditorManager.getInstance(project).openFile(virtualFile, true)
      fileToActivate = virtualFile
    }
    EduUsagesCollector.taskNavigation()
    if (fileToActivate != null) {
      updateProjectView(project, fileToActivate)
    }

    EduUtils.selectFirstAnswerPlaceholder(EduUtils.getSelectedStudyEditor(project), project)
    ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)?.hide(null)
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
