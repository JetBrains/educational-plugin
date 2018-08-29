package com.jetbrains.edu.learning.courseFormat

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.intellij.util.xmlb.annotations.Transient

abstract class StudyFile {
  @Expose
  @SerializedName("is_visible")
  var isVisible: Boolean = true

  @Transient
  @Expose
  @SerializedName("text")
  private var _text: String = ""

  fun getText(): String = _text
  fun setText(text: String?) {
    _text = text ?: ""
  }
}
