package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_HTML
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_MD

enum class DescriptionFormat(val descriptionFileName: String) {
  HTML(TASK_HTML),
  MD(TASK_MD);

  val fileExtension: String = getExtension(descriptionFileName)

}
