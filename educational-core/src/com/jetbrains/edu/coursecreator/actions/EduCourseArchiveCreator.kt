package com.jetbrains.edu.coursecreator.actions

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.mixins.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.coursera.CourseraCourse
import java.text.SimpleDateFormat
import java.util.*

class EduCourseArchiveCreator(project: Project, location: String, aesKey: String?)
  : CourseArchiveCreator(project, location, aesKey) {

  override fun getMapper(course: Course): ObjectMapper = if (course.id == 0) course.localMapper else course.remoteMapper

  private val Course.localMapper: ObjectMapper
    get() {
      val factory = JsonFactory()
      val mapper = ObjectMapper(factory)
      mapper.addMixIn(CourseraCourse::class.java, CourseraCourseMixin::class.java)
      mapper.addMixIn(EduCourse::class.java, LocalEduCourseMixin::class.java)
      addStudyItemMixins(mapper)
      commonMapperSetup(mapper, course, aesKey)
      return mapper
    }

  private val Course.remoteMapper: ObjectMapper
    get() {
      val factory = JsonFactory()
      val mapper = ObjectMapper(factory)
      mapper.addMixIn(EduCourse::class.java, RemoteEduCourseMixin::class.java)
      mapper.addMixIn(Section::class.java, RemoteSectionMixin::class.java)
      mapper.addMixIn(Lesson::class.java, RemoteLessonMixin::class.java)
      mapper.addMixIn(Task::class.java, RemoteTaskMixin::class.java)
      commonMapperSetup(mapper, course, aesKey)
      val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH)
      dateFormat.timeZone = TimeZone.getTimeZone("UTC")
      mapper.dateFormat = dateFormat
      return mapper
    }
}
