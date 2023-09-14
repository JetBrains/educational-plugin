package com.jetbrains.edu.learning.newproject.ui.welcomeScreen

import com.jetbrains.edu.learning.courseFormat.Course

// BACKCOMPACT: 2023.1
fun isApplicable(): Boolean = true

fun isFromMyCoursesPage(course: Course): Boolean = course is JBACourseFromStorage
