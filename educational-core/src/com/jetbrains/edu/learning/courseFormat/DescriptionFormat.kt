package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.learning.EduNames

enum class DescriptionFormat(val descriptionFileName: String) {
  HTML(EduNames.TASK_HTML),
  MD(EduNames.TASK_MD);

  val fileExtension: String = FileUtilRt.getExtension(descriptionFileName)

}
