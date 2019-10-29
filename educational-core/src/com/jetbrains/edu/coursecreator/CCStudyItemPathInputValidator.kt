package com.jetbrains.edu.coursecreator

import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import com.jetbrains.edu.coursecreator.actions.StudyItemType
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator

class CCStudyItemPathInputValidator @JvmOverloads constructor(
  private val course: Course,
  private val studyItemType: StudyItemType?,
  private val parentDir: VirtualFile?,
  private val name: String? = null
) : InputValidatorEx {

  private var errorText: String? = null

  override fun checkInput(inputString: String): Boolean {
    errorText = when {
      parentDir == null -> "Invalid parent directory"
      inputString.isEmpty() -> "Empty name"
      !PathUtil.isValidFileName(inputString) -> "Invalid name"
      parentDir.findChild(inputString) != null && inputString != name -> "${parentDir.name} already contains directory named $inputString"
      studyItemType != null -> course.configurator?.courseBuilder?.validateItemName(inputString, studyItemType)
      else -> null
    }
    return errorText == null
  }

  override fun canClose(inputString: String): Boolean = checkInput(inputString)

  override fun getErrorText(inputString: String): String? = errorText
}
