package com.jetbrains.edu.learning.courseFormat

import com.google.gson.annotations.SerializedName
import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.learning.EduNames

enum class DescriptionFormat(val descriptionFileName: String) {
  @SerializedName("HTML") HTML(EduNames.TASK_HTML),
  @SerializedName("MD") MD(EduNames.TASK_MD);

  val fileExtension: String = FileUtilRt.getExtension(descriptionFileName)

  override fun toString(): String {
    return name
  }

}
