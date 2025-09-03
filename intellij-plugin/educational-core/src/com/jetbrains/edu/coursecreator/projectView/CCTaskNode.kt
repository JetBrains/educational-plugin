package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.jetbrains.edu.learning.configuration.CourseViewVisibility
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isTestsFile
import com.jetbrains.edu.learning.projectView.CourseViewUtils.courseViewVisibilityAttribute
import com.jetbrains.edu.learning.projectView.TaskNode

class CCTaskNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  task: Task
) : TaskNode(project, value, viewSettings, task) {

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val node = super.modifyChildNode(childNode)
    if (node != null) {
      return node
    }
    val value = childNode.value

    val course = project?.course ?: return null
    val configurator = course.configurator ?: return null
    val visibility = childNode.courseViewVisibilityAttribute(project, course)
    if (visibility == CourseViewVisibility.INVISIBLE_FOR_ALL) return null

    if (value is PsiDirectory) {
      return createChildDirectoryNode(value)
    }
    else if (value is PsiElement) {
      val psiFile = value.containingFile
      val virtualFile = psiFile.virtualFile ?: return null
      return if (!virtualFile.isTestsFile(myProject)) {
        CCStudentInvisibleFileNode(myProject, psiFile, settings)
      }
      else {
        CCStudentInvisibleFileNode(myProject, psiFile, settings, getTestNodeName(configurator, value))
      }
    }
    return null
  }

  override fun createChildDirectoryNode(value: PsiDirectory): PsiDirectoryNode {
    return CCNode(myProject, value, settings, item)
  }

  override fun createChildFileNode(originalNode: AbstractTreeNode<*>, psiFile: PsiFile): PsiFileNode {
    return CCFileNode(myProject, psiFile, settings)
  }

  companion object {
    private fun getTestNodeName(configurator: EduConfigurator<*>, psiElement: PsiElement): String {
      val defaultTestName = configurator.testFileName
      if (psiElement is PsiFile) {
        return defaultTestName
      }
      if (psiElement is PsiNamedElement) {
        val name = psiElement.name
        return name ?: defaultTestName
      }
      return defaultTestName
    }
  }
}
