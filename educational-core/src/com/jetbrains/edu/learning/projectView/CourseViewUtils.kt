package com.jetbrains.edu.learning.projectView

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.intellij.generation.EduGradleUtils

object CourseViewUtils {

  @JvmStatic
  inline fun modifyTaskChildNode(
    project: Project,
    childNode: AbstractTreeNode<*>,
    task: Task,
    directoryNodeFactory: (PsiDirectory) -> AbstractTreeNode<*>
  ): AbstractTreeNode<*>? {
    val value = childNode.value
    return when (value) {
      is PsiDirectory -> {
        val dirName = value.name
        if (dirName == EduNames.BUILD || dirName == EduNames.OUT) return null
        val sourceDir = task.sourceDir
        if (dirName != sourceDir) directoryNodeFactory(value) else null
      }
      is PsiElement -> {
        val psiFile = value.containingFile ?: return null
        val virtualFile = psiFile.virtualFile ?: return null
        if (EduUtils.getTaskFile(project, virtualFile) != null) childNode else null
      }
      else -> null
    }
  }

  @JvmStatic
  fun findTaskDirectory(project: Project, baseDir: PsiDirectory, task: Task): PsiDirectory? {
    val sourceDirName = task.sourceDir
    if (!sourceDirName.isNullOrEmpty()) {
      val isCourseCreatorGradleProject = EduGradleUtils.isConfiguredWithGradle(project) && CCUtils.isCourseCreator(project)
      if (!isCourseCreatorGradleProject) {
        val vFile = baseDir.virtualFile
        val sourceVFile = vFile.findFileByRelativePath(sourceDirName!!) ?: return baseDir
        return PsiManager.getInstance(project).findDirectory(sourceVFile)
      }
    }
    return baseDir
  }
}
