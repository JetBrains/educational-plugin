package com.jetbrains.edu.coursecreator.projectView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.edu.learning.canBeAddedToTask
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.pathInCourse
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.gradle.GradleConstants.LOCAL_PROPERTIES
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.projectView.CourseViewUtils.testPresentation
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.isConfigFile
import org.jetbrains.annotations.TestOnly

/**
 * Add to the file name postfix "course.creator.course.view.excluded" from EduCoreBundle.properties
 * if the file in .courseignore
 */
class CCStudentInvisibleFileNode(
  project: Project,
  value: PsiFile,
  viewSettings: ViewSettings,
  private val name: String = value.name
) : CCFileNode(project, value, viewSettings) {

  private fun needsExcludedMark(file: VirtualFile?, project: Project): Boolean {
    file ?: return false
    val task = file.getContainingTask(project)

    return if (task != null) {
      file.canBeAddedToTask(project)
    }
    else {
      val course = project.course ?: return false
      !containsAdditionalFile(course, file) && !generatedPersonallyForStudent(file)
    }
  }

  override fun updateImpl(data: PresentationData) {
    super.updateImpl(data)

    val file = value.virtualFile
    val isExcluded = needsExcludedMark(file, project)
    val presentableName = if (isExcluded) message("course.creator.course.view.excluded", name) else name

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

  private fun containsAdditionalFile(course: Course, file: VirtualFile): Boolean {
    val relativePath = file.pathInCourse(project) ?: return false
    return course.additionalFiles.any { it.name == relativePath }
  }

  private fun generatedPersonallyForStudent(file: VirtualFile): Boolean =
    // TODO should be delegated to [configurator] after EDU-7821 is implemented
    // task.md and task.html are also generated personally for a student, but this method is called only for files outside tasks
    isConfigFile(file)
    || file.name == LOCAL_PROPERTIES // for android configurator
    || file.extension?.lowercase() == "sln" // for C# configurator
}
