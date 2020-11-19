package com.jetbrains.edu.learning.newproject.coursesStorage

interface CourseDeletedListener {
  fun courseDeleted(course: CourseMetaInfo)
}