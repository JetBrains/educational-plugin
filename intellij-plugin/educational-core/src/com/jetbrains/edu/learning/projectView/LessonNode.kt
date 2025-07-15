package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.projectView.CourseViewUtils.findTaskDirectory
import com.jetbrains.edu.learning.projectView.CourseViewUtils.modifyAdditionalFileOrDirectoryForLearner

open class LessonNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  lesson: Lesson
) : EduNode<Lesson>(project, value, viewSettings, lesson) {

  override fun getWeight(): Int = item.index

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    return getTaskNode(childNode) ?: childNode.modifyAdditionalFileOrDirectoryForLearner(project, item.course, showUserCreatedFiles = false)
  }

  private fun getTaskNode(childNode: AbstractTreeNode<*>): TaskNode? {
    val directory = childNode.value as? PsiDirectory ?: return null
    val task = item.getTask(directory.name) ?: return null
    val taskDirectory = findTaskDirectory(myProject, directory, task) ?: return null
    return createTaskNode(taskDirectory, task)
  }

  protected open fun createTaskNode(directory: PsiDirectory, task: Task): TaskNode {
    return TaskNode(myProject, directory, settings, task)
  }

  override val item: Lesson get() = super.item!!
}
