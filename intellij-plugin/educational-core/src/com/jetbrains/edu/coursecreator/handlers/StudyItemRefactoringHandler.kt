package com.jetbrains.edu.coursecreator.handlers

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiReference
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task

interface StudyItemRefactoringHandler {
  fun beforeMoveTask(project: Project, task: Task, newParent: VirtualFile)
  fun beforeMoveLesson(project: Project, lesson: Lesson, newParent: VirtualFile)
  fun beforeRenameStudyItem(project: Project, item: StudyItem, newName: String)
  fun processReferences(references: MutableCollection<PsiReference>): MutableCollection<PsiReference>

  companion object {
    val EP_NAME = ExtensionPointName.create<StudyItemRefactoringHandler>("Educational.studyItemRefactoringHandler")

    fun processBeforeTaskMovement(project: Project, task: Task, newParent: VirtualFile) {
      EP_NAME.extensionsIfPointIsRegistered.firstOrNull()?.beforeMoveTask(project, task, newParent)
    }

    fun processBeforeLessonMovement(project: Project, lesson: Lesson, newParent: VirtualFile) {
      EP_NAME.extensionsIfPointIsRegistered.firstOrNull()?.beforeMoveLesson(project, lesson, newParent)
    }

    fun processBeforeRename(project: Project, item: StudyItem, newName: String) {
      EP_NAME.extensionsIfPointIsRegistered.firstOrNull()?.beforeRenameStudyItem(project, item, newName)
    }

    fun processUsageReferences(references: MutableCollection<PsiReference>): MutableCollection<PsiReference> {
      return EP_NAME.extensionsIfPointIsRegistered.firstOrNull()?.processReferences(references) ?: references
    }
  }
}