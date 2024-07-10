package com.jetbrains.edu.coursecreator.handlers.move

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.refactoring.move.MoveCallback
import com.jetbrains.edu.coursecreator.CCUtils.updateHigherElements
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.saveItem
import java.io.IOException

class CCTaskMoveHandlerDelegate : CCStudyItemMoveHandlerDelegate(StudyItemType.TASK_TYPE) {
  override fun isAvailable(directory: PsiDirectory): Boolean {
    return directory.virtualFile.isTaskDirectory(directory.project)
  }

  override fun doMove(
    project: Project,
    elements: Array<PsiElement>,
    targetContainer: PsiElement?,
    callback: MoveCallback?
  ) {
    if (targetContainer !is PsiDirectory) {
      return
    }
    val targetVFile = targetContainer.virtualFile
    if (!targetVFile.isTaskDirectory(project) && !targetVFile.isLessonDirectory(project)) {
      Messages.showInfoMessage(
        message("dialog.message.incorrect.movement.task"),
        message("dialog.title.incorrect.target.for.move")
      )
      return
    }
    StudyTaskManager.getInstance(project).course ?: return
    val sourceDirectory = elements[0] as PsiDirectory
    val taskToMove = sourceDirectory.virtualFile.getTask(project) ?: return
    val sourceLesson = taskToMove.lesson
    if (targetVFile.isLessonDirectory(project)) {
      //if user moves task to any lesson, this task is inserted as the last task in this lesson
      val targetLesson = targetVFile.getLesson(project) ?: return
      if (targetVFile.findChild(taskToMove.name) != null) {
        Messages.showInfoMessage(
          message("dialog.message.task.name.conflict.in.lesson"),
          message("dialog.title.incorrect.target.for.move")
        )
        return
      }
      val taskList = targetLesson.taskList
      val targetTask = if (taskList.isEmpty()) null else taskList[taskList.size - 1]
      moveTask(sourceDirectory, taskToMove, targetTask, 1, targetVFile, targetLesson)
      saveItem(sourceLesson)
      saveItem(targetLesson)
    }
    else {
      val lessonDir = targetVFile.parent ?: return
      val targetTask = targetVFile.getTask(project) ?: return
      val delta = getDelta(project, targetTask) ?: return
      moveTask(sourceDirectory, taskToMove, targetTask, delta, lessonDir, targetTask.lesson)
      saveItem(sourceLesson)
      saveItem(targetTask.lesson)
    }
    ProjectView.getInstance(project).refresh()
  }

  private fun moveTask(
    sourceDirectory: PsiDirectory,
    taskToMove: Task,
    targetTask: Task?,
    indexDelta: Int,
    targetDirectory: VirtualFile,
    targetLesson: Lesson
  ) {
    val sourceLessonDir = sourceDirectory.virtualFile.parent ?: return
    ApplicationManager.getApplication().runWriteAction(object : Runnable {
      override fun run() {
        try {
          //moving file to the same directory leads to exception
          if (targetDirectory != sourceLessonDir) {
            sourceDirectory.virtualFile.move(this, targetDirectory)
          }
        }
        catch (e: IOException) {
          LOG.error(e)
        }
      }
    })
    updateHigherElements(sourceLessonDir.children, { file: VirtualFile -> taskToMove.lesson.getTask(file.name) }, taskToMove.index, -1)
    val newItemIndex = if (targetTask != null) targetTask.index + indexDelta else 1
    taskToMove.index = -1
    taskToMove.lesson.removeTask(taskToMove)
    updateHigherElements(targetDirectory.children, { file: VirtualFile -> targetLesson.getTask(file.name) }, newItemIndex - 1, 1)
    taskToMove.index = newItemIndex
    taskToMove.parent = targetLesson
    targetLesson.addTask(taskToMove)
    targetLesson.sortItems()
  }

  companion object {
    private val LOG = Logger.getInstance(CCTaskMoveHandlerDelegate::class.java)
  }
}
