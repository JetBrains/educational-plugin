package com.jetbrains.edu.learning.ui

import com.intellij.openapi.util.NlsActions
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.presentableName
import com.jetbrains.edu.learning.courseFormat.CourseraCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.messages.EduCoreBundle


fun Task.getUIName(): String = if (course is HyperskillCourse) {
  if (this is CodeTask) EduCoreBundle.message("item.task.challenge") else EduCoreBundle.message("item.task.stage")
}
else {
  StudyItemType.TASK_TYPE.presentableName
}

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
    is UnsupportedTask -> EduCoreBundle.message("hyperskill.unsupported.check.task")
    else -> defaultMessage
  }
}