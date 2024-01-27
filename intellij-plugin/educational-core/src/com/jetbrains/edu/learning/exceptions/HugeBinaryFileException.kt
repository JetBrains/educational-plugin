package com.jetbrains.edu.learning.exceptions

import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.messages.EduCoreBundle

/**
 * [HugeBinaryFileException] is similar to [com.intellij.openapi.util.io.FileTooBigException],
 * but stores additional information and generates a user visible message.
 */
class HugeBinaryFileException(val path: String, val size: Long, val limit: Long, insideFrameworkLesson: Boolean = false) : IllegalStateException(
  buildString {
    appendLine(EduCoreBundle.message("error.educator.file.size", path))
    appendLine(EduCoreBundle.message("error.educator.file.size.limit", StringUtil.formatFileSize(size), StringUtil.formatFileSize(limit)))
    if (insideFrameworkLesson && !FileUtilRt.isTooLarge(size)) {
      appendLine(EduCoreBundle.message("error.educator.file.size.workaround.framework"))
    }
    else {
      appendLine(EduCoreBundle.message("error.educator.file.size.workaround.exclude"))
    }
  }
)