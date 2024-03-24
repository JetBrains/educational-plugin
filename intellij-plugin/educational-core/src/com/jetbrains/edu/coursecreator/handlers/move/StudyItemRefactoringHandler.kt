package com.jetbrains.edu.coursecreator.handlers.move

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task

interface StudyItemRefactoringHandler {
  fun moveTask(project: Project, task: Task, newParent: VirtualFile)
  fun moveLesson(project: Project, lesson: Lesson, newParent: VirtualFile)
  fun renameStudyItem(project: Project, item: StudyItem, newName: String)

  companion object {
    val EP_NAME = ExtensionPointName.create<StudyItemRefactoringHandler>("Educational.studyItemRefactoringHandler")

    fun processBeforeTaskMovement(project: Project, task: Task, newParent: VirtualFile) {
      EP_NAME.extensionsIfPointIsRegistered.firstOrNull()?.moveTask(project, task, newParent)
    }

    fun processBeforeLessonMovement(project: Project, lesson: Lesson, newParent: VirtualFile) {
      EP_NAME.extensionsIfPointIsRegistered.firstOrNull()?.moveLesson(project, lesson, newParent)
    }

    fun processBeforeRename(project: Project, item: StudyItem, newName: String) {
      EP_NAME.extensionsIfPointIsRegistered.firstOrNull()?.renameStudyItem(project, item, newName)
    }
  }
}