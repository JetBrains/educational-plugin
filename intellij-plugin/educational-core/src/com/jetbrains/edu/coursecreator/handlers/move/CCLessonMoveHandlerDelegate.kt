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
import com.jetbrains.edu.coursecreator.handlers.StudyItemRefactoringHandler
import com.jetbrains.edu.coursecreator.ui.CCItemPositionPanel
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.getLesson
import com.jetbrains.edu.learning.isLessonDirectory
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.saveItem
import java.io.IOException

class CCLessonMoveHandlerDelegate : CCStudyItemMoveHandlerDelegate(StudyItemType.LESSON_TYPE) {
  override fun isAvailable(directory: PsiDirectory): Boolean {
    return directory.virtualFile.isLessonDirectory(directory.project)
  }

  override fun doMove(project: Project, elements: Array<PsiElement>, targetDirectory: PsiElement?, callback: MoveCallback?) {
    if (targetDirectory !is PsiDirectory) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course ?: return
    val sourceDirectory = elements[0] as PsiDirectory
    val sourceVFile = sourceDirectory.virtualFile
    val sourceLesson = sourceVFile.getLesson(project) ?: throw IllegalStateException("Failed to find lesson for `sourceVFile` directory")
    val targetVFile = targetDirectory.virtualFile
    val targetItem = getTargetItem(course, targetVFile, project)
    if (targetItem == null) {
      Messages.showInfoMessage(message("dialog.message.incorrect.movement.lesson"), message("dialog.title.incorrect.target.for.move"))
      return
    }
    val sourceParentDir = sourceVFile.parent
    val targetParentDir = if (targetItem is Lesson) targetVFile.parent else targetVFile
    if (targetItem is Section || targetItem is Course) {
      if (targetParentDir.findChild(sourceLesson.name) != null) {
        val message = if (targetItem is Section) {
          message("dialog.message.lesson.name.conflict.in.section")
        }
        else {
          message("dialog.message.lesson.name.conflict.in.course")
        }
        Messages.showInfoMessage(message, message("dialog.title.incorrect.target.for.move"))
        return
      }
    }
    val targetSection = course.getSection(targetParentDir.name)
    val targetContainer = targetSection ?: course
    var delta: Int? = CCItemPositionPanel.AFTER_DELTA
    if (targetItem is Lesson) {
      delta = getDelta(project, targetItem)
    }
    if (delta == null) {
      return
    }
    StudyItemRefactoringHandler.processBeforeLessonMovement(project, sourceLesson, targetParentDir)
    val sourceContainer = sourceLesson.container
    val sourceLessonIndex = sourceLesson.index
    sourceLesson.index = -1
    updateHigherElements(sourceParentDir.children, { file: VirtualFile -> sourceContainer.getItem(file.name) }, sourceLessonIndex, -1)
    val newItemIndex = ((targetItem as? Lesson)?.index ?: (targetItem as ItemContainer).items.size) + delta
    updateHigherElements(targetParentDir.children, { file: VirtualFile -> targetContainer.getItem(file.name) }, newItemIndex - 1, 1)
    sourceLesson.index = newItemIndex
    sourceLesson.parent = targetContainer
    sourceContainer.removeLesson(sourceLesson)
    targetContainer.addLesson(sourceLesson)
    course.sortItems()
    ApplicationManager.getApplication().runWriteAction(object : Runnable {
      override fun run() {
        try {
          if (targetParentDir != sourceVFile.parent) {
            sourceVFile.move(this, targetParentDir)
          }
        }
        catch (e: IOException) {
          LOG.error(e)
        }
      }
    })
    ProjectView.getInstance(project).refresh()
    saveItem(targetContainer)
    saveItem(sourceContainer)
  }

  companion object {
    private val LOG = Logger.getInstance(CCLessonMoveHandlerDelegate::class.java)

    private fun getTargetItem(course: Course, targetVFile: VirtualFile, project: Project): StudyItem? {
      if (targetVFile == project.courseDir) return course
      return course.getItem(targetVFile.name) ?: targetVFile.getLesson(project)
    }
  }
}
