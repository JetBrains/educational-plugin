package com.jetbrains.edu.learning.ui

import com.intellij.openapi.util.NlsActions
import com.jetbrains.edu.learning.courseFormat.CourseraCourse
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.messages.EduCoreBundle

@NlsActions.ActionText
fun Task.getUICheckLabel(): String {
  val defaultMessage = if (course is CourseraCourse) {
    if ((course as CourseraCourse).submitManually) {
      EduCoreBundle.message("action.coursera.run.tests.text")
    }
    else {
      EduCoreBundle.message("action.coursera.submit.text")
    }
  }
  else EduCoreBundle.message("action.Educational.Check.text")

  return when (this) {
    is TheoryTask -> EduCoreBundle.message("action.check.run.text")
    is DataTask -> EduCoreBundle.message("send.answer")
    is UnsupportedTask -> EduCoreBundle.message("unsupported.check.task")
    else -> defaultMessage
  }
}