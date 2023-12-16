package com.jetbrains.edu.learning.newproject.coursesStorage

import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.JBACourseFromStorage

interface CourseDeletedListener {
  fun courseDeleted(course: JBACourseFromStorage)
}