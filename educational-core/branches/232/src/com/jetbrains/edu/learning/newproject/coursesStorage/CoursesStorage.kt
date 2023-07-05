package com.jetbrains.edu.learning.newproject.coursesStorage

import com.intellij.openapi.components.*
import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CourseDataStorage
import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CourseInfo
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.CourseMetaInfo

@State(name = "CoursesStorage", storages = [Storage("coursesStorage.xml", roamingType = RoamingType.DISABLED)])
@Service
class CoursesStorage : CourseDataStorage, CoursesStorageBase() {

  override fun getCoursePath(course: Course): String? = getCoursePath(CourseMetaInfo().apply {
    this.name = course.name
    this.id = course.id
    this.courseMode = course.courseMode
    this.languageId = course.languageId
    this.languageVersion = course.languageVersion
  })

  private fun getCourseMetaInfo(name: String, id: Int, courseMode: CourseMode, languageId: String): CourseMetaInfo? {
    return state.courses.find {
      it.name == name
      && it.id == id
      && it.courseMode == courseMode
      && it.languageId == languageId
    }
  }

  override fun getCoursePath(courseInfo: CourseInfo): String? {
    return if (courseInfo is CourseMetaInfo) {
      getCourseMetaInfo(courseInfo.name, courseInfo.id, courseInfo.courseMode, courseInfo.languageId)?.location
    }
    else {
      null
    }
  }

  override fun removeCourseByLocation(location: String): Boolean {
    return super.doRemoveCourseByLocation(location)
  }

  override fun getAllCourses(): List<CourseInfo> {
    return state.courses
  }

  companion object {
    fun getInstance(): CoursesStorage = service()
  }
}