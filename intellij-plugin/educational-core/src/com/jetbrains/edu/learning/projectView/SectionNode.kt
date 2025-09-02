package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.projectView.CourseViewUtils.modifyAdditionalFileOrDirectory
import com.jetbrains.edu.learning.projectView.FrameworkLessonNode.Companion.createFrameworkLessonNode

open class SectionNode(
  project: Project,
  viewSettings: ViewSettings,
  section: Section,
  psiDirectory: PsiDirectory
) : EduNode<Section>(project, psiDirectory, viewSettings, section) {

  override val item: Section get() = super.item!!

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    return getLessonNode(childNode) ?: childNode.modifyAdditionalFileOrDirectory(project, item.course, showUserCreatedFiles = false)
  }

  private fun getLessonNode(childNode: AbstractTreeNode<*>): LessonNode? {
    val directory = childNode.value as? PsiDirectory ?: return null
    val lesson = item.getLesson(directory.name) ?: return null
    return createLessonNode(directory, lesson)
  }

  protected open fun createLessonNode(directory: PsiDirectory, lesson: Lesson): LessonNode? {
    return if (lesson is FrameworkLesson) {
      createFrameworkLessonNode(myProject, directory, settings, lesson)
    }
    else {
      LessonNode(myProject, directory, settings, lesson)
    }
  }

  override fun getWeight(): Int = item.index
}
