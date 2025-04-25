package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.projectView.CourseViewUtils.createNodeFromPsiDirectory

/**
 *  We should not pass a course as `[item]` here, otherwise this node will be treated
 *  as a course node and have the course icon and course name
 **/
open class IntermediateDirectoryNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  private val course: Course
) : ContentHolderNode, EduNode<Nothing>(project, value, viewSettings, null) {
  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val directory = childNode.value as? PsiDirectory ?: return null
    return createNodeFromPsiDirectory(course, directory) ?: childNode
  }
}