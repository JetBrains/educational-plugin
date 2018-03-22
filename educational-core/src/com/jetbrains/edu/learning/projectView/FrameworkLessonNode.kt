package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson

class FrameworkLessonNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  lesson: FrameworkLesson
) : LessonNode(project, value, viewSettings, lesson) {

  override fun getLesson(): FrameworkLesson = super.getLesson() as FrameworkLesson

  override fun updateImpl(data: PresentationData) {
    super.updateImpl(data)
    data.addText("  ${lesson.currentTaskIndex + 1}/${lesson.taskList.size}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
  }

  override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> {
    val taskNode = ProjectViewDirectoryHelper.getInstance(myProject)
                     .getDirectoryChildren(value, settings, true) { item -> item.isDirectory && item.name == EduNames.TASK }
                     .firstOrNull() ?: return emptyList()
    val task = lesson.currentTask()
    val taskDirectory = (taskNode.value as? PsiDirectory)?.let { CourseViewUtils.findTaskDirectory(myProject, it, task) } ?: return emptyList()
    return ProjectViewDirectoryHelper.getInstance(myProject).getDirectoryChildren(taskDirectory, settings, true)
      .mapNotNull { child ->
        CourseViewUtils.modifyTaskChildNode(myProject, child, task) { dir ->
          DirectoryNode(myProject, dir, settings)
        }
      }
  }
}
