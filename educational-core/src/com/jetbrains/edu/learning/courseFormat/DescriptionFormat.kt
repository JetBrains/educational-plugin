package com.jetbrains.edu.learning.courseFormat

import com.google.gson.annotations.SerializedName
import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.learning.EduNames

enum class DescriptionFormat(val descriptionFileName: String) {
  @SerializedName("html") HTML(EduNames.TASK_HTML),
  @SerializedName("md") MD(EduNames.TASK_MD);

  val fileExtension: String = FileUtilRt.getExtension(descriptionFileName)
}
