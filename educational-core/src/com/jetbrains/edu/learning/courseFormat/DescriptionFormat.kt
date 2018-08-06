package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.learning.EduNames

enum class DescriptionFormat(val descriptionFileName: String) {
  @JsonProperty("html") @SerializedName("html") HTML(EduNames.TASK_HTML),
  @JsonProperty("md") @SerializedName("md") MD(EduNames.TASK_MD);

  val fileExtension: String = FileUtilRt.getExtension(descriptionFileName)
}
