package com.jetbrains.edu.coursecreator.actions

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.mixins.MarketplaceCourseMixin
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import java.io.File

class MarketplaceArchiveCreator(project: Project, zipFile: File)
  : CourseArchiveCreator(project, zipFile) {

  override fun getMapper(course: Course): ObjectMapper = course.localMapper

  companion object {

    private val Course.localMapper: ObjectMapper
      get() {
        val factory = JsonFactory()
        val mapper = ObjectMapper(factory)
        mapper.addMixIn(EduCourse::class.java, MarketplaceCourseMixin::class.java)
        addStudyItemMixins(mapper)
        commonMapperSetup(mapper, course)
        return mapper
      }

  }
}
