package com.jetbrains.edu.coursecreator.projectView

import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.projectView.IntermediateDirectoryNode
import com.jetbrains.edu.learning.projectView.LessonNode
import com.jetbrains.edu.learning.projectView.SectionNode
import com.jetbrains.edu.learning.projectView.ContentHolderNode

interface CCContentHolderNode : ContentHolderNode {
  override fun createLessonNode(directory: PsiDirectory, lesson: Lesson): LessonNode {
    return CCLessonNode(getProject(), directory, getSettings(), lesson)
  }

  override fun createSectionNode(directory: PsiDirectory, section: Section): SectionNode {
    return CCSectionNode(getProject(), getSettings(), section, directory)
  }

  override fun createIntermediateDirectoryNode(directory: PsiDirectory, course: Course): IntermediateDirectoryNode {
    return CCIntermediateDirectoryNode(getProject(), course, directory, getSettings())
  }
}