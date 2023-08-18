package com.jetbrains.edu.learning.ui

import com.intellij.openapi.util.NlsActions
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.presentableName
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTaskWithFileIO
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.UnsupportedTask
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse


fun Task.getUIName(): String = if (course is HyperskillCourse) {
  if (this is CodeTask) EduCoreBundle.message("item.task.challenge") else EduCoreBundle.message("item.task.stage")
}
else {
  StudyItemType.TASK_TYPE.presentableName
}

@NlsActions.ActionText
fun Task.getUICheckLabel(): String {
  val defaultMessage = when (course) {
    is CourseraCourse -> {
      if ((course as CourseraCourse).submitManually) EduCoreBundle.message("action.coursera.run.tests.text")
      else EduCoreBundle.message("action.coursera.submit.text")
    }
    is CodeforcesCourse -> EduCoreBundle.message("action.codeforces.run.local.tests.text")
    else -> EduCoreBundle.message("action.check.text")
  }

  return when (this) {
    is TheoryTask -> EduCoreBundle.message("action.check.run.text")
    is CodeforcesTaskWithFileIO -> EduCoreBundle.message("codeforces.copy.and.submit")
    is DataTask -> EduCoreBundle.message("send.answer")
    is UnsupportedTask -> EduCoreBundle.message("hyperskill.unsupported.check.task")
    else -> defaultMessage
  }
}