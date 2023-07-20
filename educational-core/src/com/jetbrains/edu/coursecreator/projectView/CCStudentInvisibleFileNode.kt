package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.edu.coursecreator.courseignore.CourseIgnoreRules
import com.jetbrains.edu.learning.canBeAddedToTask
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.projectView.CourseViewUtils.testPresentation
import org.jetbrains.annotations.TestOnly

/**
 * Add to the file name postfix "course.creator.course.view.excluded" from EduCoreBundle.properties
 * if the file in .courseignore
 */
class CCStudentInvisibleFileNode(
  project: Project,
  value: PsiFile,
  viewSettings: ViewSettings,
  courseIgnoreRules: CourseIgnoreRules,
  name: String = value.name
) : PsiFileNode(project, value, viewSettings) {

  val presentableName: String
  init {
    val file = value.virtualFile
    val isExcluded = file != null && (file.canBeAddedToTask(project) || courseIgnoreRules.isIgnored(file, project))
    presentableName = if (isExcluded) message("course.creator.course.view.excluded", name) else name
  }

  override fun updateImpl(data: PresentationData) {
    super.updateImpl(data)
    data.clearText()
    data.addText(presentableName, SimpleTextAttributes.GRAY_ATTRIBUTES)
  }

  @Deprecated("Deprecated in Java",
    ReplaceWith("testPresentation(this)", "com.jetbrains.edu.learning.projectView.CourseViewUtils.testPresentation")
  )
  @TestOnly
  override fun getTestPresentation(): String {
    return testPresentation(this)
  }

}
