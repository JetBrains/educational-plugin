package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.projectView.CourseViewUtils.createNodeFromPsiDirectory
import com.jetbrains.edu.learning.projectView.CourseViewUtils.modifyAdditionalFileOrDirectoryForLearner

open class CourseNode(
  project: Project,
  value: PsiDirectory,
  settings: ViewSettings,
  course: Course
) : ContentHolderNode, EduNode<Course>(project, value, settings, course) {

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    return getStudyItemOrIntermediateDirectoryNode(childNode) ?: childNode.modifyAdditionalFileOrDirectoryForLearner(project, item, showUserCreatedFiles = false)
  }

  private fun getStudyItemOrIntermediateDirectoryNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val directory = childNode.value as? PsiDirectory ?: return null
    val node = createNodeFromPsiDirectory(item, directory)
    if (node != null) {
      return node
    }
    return null
  }

  override val additionalInfo: String?
    get() {
      if (item is HyperskillCourse) {
        return null
      }
      val (tasksSolved, tasksTotal) = ProgressUtil.countProgress(item)
      return " $tasksSolved/$tasksTotal"
    }

  override val item: Course get() = super.item!!
}
