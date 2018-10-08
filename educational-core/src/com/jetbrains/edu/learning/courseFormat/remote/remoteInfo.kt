package com.jetbrains.edu.learning.courseFormat.remote

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Tag

interface RemoteInfo

class LocalInfo : RemoteInfo

interface CourseRemoteInfo : RemoteInfo {

  fun getTags(): List<Tag> = listOf()
  fun isCourseValid(course: Course): Boolean = true
}

class LocalCourseInfo : CourseRemoteInfo
