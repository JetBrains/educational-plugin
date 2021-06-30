package com.jetbrains.edu.learning.codeforces.newProjectUI

import com.jetbrains.edu.learning.codeforces.CodeforcesPlatformProvider
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel

class CodeforcesCoursePanel : CoursePanel(false) {
  override val startButtonText: String
    get() = EduCoreBundle.message("course.dialog.start.button.codeforces.practice")


  override fun joinCourseAction(info: CourseInfo, mode: CourseMode) {
    CodeforcesPlatformProvider().joinAction(info, mode, this)
  }
}