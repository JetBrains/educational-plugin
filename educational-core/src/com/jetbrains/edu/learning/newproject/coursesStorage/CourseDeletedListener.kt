package com.jetbrains.edu.learning.newproject.coursesStorage

import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.CourseMetaInfo

interface CourseDeletedListener {
  fun courseDeleted(course: CourseMetaInfo)
}