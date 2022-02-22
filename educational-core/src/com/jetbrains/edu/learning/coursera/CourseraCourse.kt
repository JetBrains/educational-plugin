package com.jetbrains.edu.learning.coursera

import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CourseraCourse : Course() {
  var submitManually = false

  override fun getItemType(): String = CourseraNames.COURSE_TYPE
  override val checkAction: CheckAction
    get() = CheckAction(if (submitManually) EduCoreBundle.lazyMessage("action.coursera.run.tests.text")
                        else EduCoreBundle.lazyMessage("action.coursera.submit.text"))
}
