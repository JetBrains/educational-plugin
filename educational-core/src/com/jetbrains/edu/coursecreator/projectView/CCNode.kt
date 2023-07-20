package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.coursecreator.courseignore.CourseIgnoreRules
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isTestsFile
import com.jetbrains.edu.learning.projectView.DirectoryNode

class CCNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  private val courseIgnoreRules: CourseIgnoreRules,
  task: Task?
) : DirectoryNode(project, value, viewSettings, task) {

  override fun canNavigate(): Boolean = true

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val node = super.modifyChildNode(childNode)
    if (node != null) return node
    val value = childNode.value

    if (value is PsiDirectory) {
      return CCNode(myProject, value, settings, courseIgnoreRules, item)
    }
    if (value is PsiElement) {
      val psiFile = value.containingFile
      val virtualFile = psiFile.virtualFile
      val course = StudyTaskManager.getInstance(myProject).course ?: return null
      if (course.configurator == null) return CCStudentInvisibleFileNode(myProject, psiFile, settings, courseIgnoreRules)
      if (EduUtilsKt.isTaskDescriptionFile(virtualFile.name)) {
        return null
      }
      if (!virtualFile.isTestsFile(myProject)) {
        return CCStudentInvisibleFileNode(myProject, psiFile, settings, courseIgnoreRules)
      }
      else {
        if (isCourseCreator(myProject)) {
          return CCStudentInvisibleFileNode(myProject, psiFile, settings, courseIgnoreRules)
        }
      }
    }
    return null
  }

  override fun createChildDirectoryNode(value: PsiDirectory): PsiDirectoryNode {
    return CCNode(myProject, value, settings, courseIgnoreRules, item)
  }
}
