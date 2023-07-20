package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.coursecreator.courseignore.CourseIgnoreRules
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.projectView.LessonNode
import com.jetbrains.edu.learning.projectView.SectionNode

class CCSectionNode(
  project: Project,
  viewSettings: ViewSettings,
  section: Section,
  private val courseIgnoreRules: CourseIgnoreRules,
  psiDirectory: PsiDirectory
) : SectionNode(project, viewSettings, section, psiDirectory) {

  override fun createLessonNode(directory: PsiDirectory, lesson: Lesson): LessonNode {
    return CCLessonNode(myProject, directory, settings, courseIgnoreRules, lesson)
  }

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val node = super.modifyChildNode(childNode)
    return node ?: modifyNodeInEducatorMode(myProject, settings, courseIgnoreRules, childNode)
  }
}
