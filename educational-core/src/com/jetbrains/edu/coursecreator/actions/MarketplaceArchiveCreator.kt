package com.jetbrains.edu.coursecreator.actions

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.mixins.MarketplaceCourseMixin
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse

class MarketplaceArchiveCreator(project: Project, location: String, aesKey: String?)
  : CourseArchiveCreator(project, location, aesKey) {

  override fun getMapper(course: Course): ObjectMapper = course.localMapper

  private val Course.localMapper: ObjectMapper
    get() {
      val factory = JsonFactory()
      val mapper = ObjectMapper(factory)
      mapper.addMixIn(EduCourse::class.java, MarketplaceCourseMixin::class.java)
      addStudyItemMixins(mapper)
      commonMapperSetup(mapper, course, aesKey)
      return mapper
    }

}
