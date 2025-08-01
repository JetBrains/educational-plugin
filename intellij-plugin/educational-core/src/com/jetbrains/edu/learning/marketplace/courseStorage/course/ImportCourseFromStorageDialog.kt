package com.jetbrains.edu.learning.marketplace.courseStorage.course

import com.jetbrains.edu.learning.marketplace.courseStorage.COURSE_STORAGE
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.marketplace.api.EduCourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCourseDialog

class ImportCourseFromStorageDialog(courseConnector: EduCourseConnector) : ImportCourseDialog() {
  override val coursePanel = ImportCourseFromStoragePanel(courseConnector)

  init {
    title = message("dialog.title.start.course", COURSE_STORAGE)
    init()
  }
}