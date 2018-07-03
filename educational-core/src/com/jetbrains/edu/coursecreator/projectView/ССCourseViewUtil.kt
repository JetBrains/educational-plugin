@file:JvmName("CCCourseViewUtil")

package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSynchronizer


fun produceConfigNodeOrNull(project: Project, viewSettings: ViewSettings, childNode: AbstractTreeNode<*>): AbstractTreeNode<PsiFile>? {
  val value = childNode.value
  return if (value is PsiFile && YamlFormatSynchronizer.isConfigFile(value.virtualFile))
    CCStudentInvisibleFileNode(project, value, viewSettings)
  else null
}