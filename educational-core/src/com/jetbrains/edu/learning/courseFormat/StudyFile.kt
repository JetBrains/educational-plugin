package com.jetbrains.edu.learning.courseFormat

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

abstract class StudyFile {
  @Expose
  @SerializedName("is_visible")
  var isVisible: Boolean = true
  @Expose
  var text: String = ""
}
