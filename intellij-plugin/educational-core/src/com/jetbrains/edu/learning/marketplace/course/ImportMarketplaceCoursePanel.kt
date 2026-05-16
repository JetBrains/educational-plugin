package com.jetbrains.edu.learning.marketplace.course

import com.jetbrains.edu.learning.marketplace.api.EduCourseConnector
import com.jetbrains.edu.learning.newproject.course.ImportCoursePanel

class ImportMarketplaceCoursePanel(
  courseConnector: EduCourseConnector
) : ImportCoursePanel(courseConnector, "https://plugins.jetbrains.com/plugin/*")