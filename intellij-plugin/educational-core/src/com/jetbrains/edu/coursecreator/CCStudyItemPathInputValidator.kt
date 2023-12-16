package com.jetbrains.edu.coursecreator

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CCStudyItemPathInputValidator(
  private val project: Project,
  private val course: Course,
  private val studyItemType: StudyItemType?,
  private val parentDir: VirtualFile?,
  private val name: String? = null
) : InputValidatorEx {

  private var errorText: String? = null

  override fun checkInput(inputString: String): Boolean {
    errorText = when {
      parentDir == null -> EduCoreBundle.message("course.creator.error.invalid.parent.directory")
      inputString.isEmpty() -> EduCoreBundle.message("course.creator.error.empty.name")
      !PathUtil.isValidFileName(inputString) -> EduCoreBundle.message("course.creator.error.invalid.name")
      parentDir.findChild(inputString) != null && inputString != name ->
        EduCoreBundle.message("course.creator.error.duplicated.name", parentDir.name, inputString)
      studyItemType != null -> course.configurator?.courseBuilder?.validateItemName(project, inputString, studyItemType)
      else -> null
    }
    return errorText == null
  }

  override fun canClose(inputString: String): Boolean = checkInput(inputString)

  override fun getErrorText(inputString: String): String? = errorText
}
