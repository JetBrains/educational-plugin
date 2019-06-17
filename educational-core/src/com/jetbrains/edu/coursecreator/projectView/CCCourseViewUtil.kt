@file:JvmName("CCCourseViewUtil")

package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile


fun modifyNodeInEducatorMode(project: Project, viewSettings: ViewSettings, childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
  val value = childNode.value
  return when (value) {
    is PsiDirectory -> CCNode(project, value, viewSettings, null)
    is PsiFile -> CCStudentInvisibleFileNode(project, value, viewSettings)
    else -> null
  }
}