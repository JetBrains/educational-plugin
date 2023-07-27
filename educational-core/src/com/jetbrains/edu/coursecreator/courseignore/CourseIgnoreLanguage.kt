package com.jetbrains.edu.coursecreator.courseignore

import com.intellij.openapi.vcs.changes.ignore.lang.IgnoreFileType
import com.intellij.openapi.vcs.changes.ignore.lang.IgnoreLanguage

object CourseIgnoreLanguage : IgnoreLanguage("CourseIgnore", "courseignore") {

  override fun getFileType(): IgnoreFileType {
    return CourseIgnoreFileType
  }

  override fun isSyntaxSupported(): Boolean = true
}