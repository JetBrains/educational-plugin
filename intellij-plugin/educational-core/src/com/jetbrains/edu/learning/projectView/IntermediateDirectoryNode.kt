package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.projectView.CourseViewUtils.extractNodeFromPsiDirectory
import com.jetbrains.edu.learning.projectView.FrameworkLessonNode.Companion.createFrameworkLessonNode

open class IntermediateDirectoryNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
) : EduNode<Nothing>(project, value, viewSettings, null) {
  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val directory = childNode.value as? PsiDirectory ?: return null
    val course = myProject.course ?: return null
    return extractNodeFromPsiDirectory(directory, course, myProject, ::createSectionNode, ::createLessonNode, ::createIntermediateDirectoryNode)
  }

  protected open fun createSectionNode(directory: PsiDirectory, section: Section): SectionNode {
    return SectionNode(myProject, settings, section, directory)
  }

  protected open fun createLessonNode(directory: PsiDirectory, lesson: Lesson): LessonNode? {
    return if (lesson is FrameworkLesson) {
      createFrameworkLessonNode(myProject, directory, settings, lesson)
    }
    else {
      LessonNode(myProject, directory, settings, lesson)
    }
  }

  protected open fun createIntermediateDirectoryNode(directory: PsiDirectory): IntermediateDirectoryNode {
    return IntermediateDirectoryNode(myProject, directory, settings)
  }
}