package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.projectView.IntermediateDirectoryNode
import com.jetbrains.edu.learning.projectView.LessonNode
import com.jetbrains.edu.learning.projectView.SectionNode

class CCIntermediateDirectoryNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
) : IntermediateDirectoryNode(project, value, viewSettings) {
  override fun createLessonNode(directory: PsiDirectory, lesson: Lesson): LessonNode {
    return CCLessonNode(myProject, directory, settings, lesson)
  }

  override fun createSectionNode(directory: PsiDirectory, section: Section): SectionNode {
    return CCSectionNode(myProject, settings, section, directory)
  }

  override fun createIntermediateDirectoryNode(directory: PsiDirectory): IntermediateDirectoryNode {
    return CCIntermediateDirectoryNode(myProject, directory, settings)
  }
}