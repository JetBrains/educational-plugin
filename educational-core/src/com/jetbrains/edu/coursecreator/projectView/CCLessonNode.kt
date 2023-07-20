package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.coursecreator.courseignore.CourseIgnoreRules
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.projectView.LessonNode
import com.jetbrains.edu.learning.projectView.TaskNode

class CCLessonNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  private val courseIgnoreRules: CourseIgnoreRules,
  lesson: Lesson
) : LessonNode(project, value, viewSettings, lesson) {
  public override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val node = super.modifyChildNode(childNode)
    return node ?: modifyNodeInEducatorMode(myProject, settings, courseIgnoreRules, childNode)
  }

  override fun createTaskNode(directory: PsiDirectory, task: Task): TaskNode {
    return CCTaskNode(myProject, directory, settings, courseIgnoreRules, task)
  }
}
