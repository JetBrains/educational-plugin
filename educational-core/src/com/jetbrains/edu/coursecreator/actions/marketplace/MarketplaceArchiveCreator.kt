package com.jetbrains.edu.coursecreator.actions.marketplace

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.CourseArchiveCreator
import com.jetbrains.edu.coursecreator.actions.mixins.*
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.encrypt.EncryptionModule
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.generateCourseItemsIds
import com.jetbrains.edu.learning.marketplace.isMarketplaceRemoteCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle

class MarketplaceArchiveCreator(val project: Project, location: String, aesKey: String?)
  : CourseArchiveCreator(project, location, aesKey) {

  override fun getMapper(course: Course): ObjectMapper = course.localMapper

  private val Course.localMapper: ObjectMapper
    get() {
      val factory = JsonFactory()
      val mapper = ObjectMapper(factory)
      mapper.addMixIn(EduCourse::class.java, MarketplaceCourseMixin::class.java)
      addStudyItemMixins(mapper)
      mapper.registerModule(EncryptionModule(aesKey))
      commonMapperSetup(mapper, course)
      return mapper
    }

  override fun compute(): String? {
    val course = StudyTaskManager.getInstance(project).course ?: return EduCoreBundle.message("error.unable.to.obtain.course.for.project")
    if (!course.isMarketplaceRemoteCourse() && !isUnitTestMode) {
      course.generateCourseItemsIds()
    }

    return super.compute()
  }

  override fun validateCourse(course: Course): String? {
    val errors = super.validateCourse(course)
    if (errors != null) return errors
    if (course.vendor == null) {
      return "Course vendor is empty. Please, add vendor to the course.yaml"
    }
    return null
  }

  override fun addStudyItemMixins(mapper: ObjectMapper) {
    mapper.addMixIn(FrameworkLesson::class.java, FrameworkLessonMixin::class.java)
    mapper.addMixIn(ChoiceTask::class.java, ChoiceTaskLocalMixin::class.java)
    mapper.addMixIn(ChoiceOption::class.java, ChoiceOptionLocalMixin::class.java)
    mapper.addMixIn(Section::class.java, RemoteMarketplaceSectionMixin::class.java)
    mapper.addMixIn(Lesson::class.java, RemoteMarketplaceLessonMixin::class.java)
    mapper.addMixIn(Task::class.java, RemoteMarketplaceTaskMixin::class.java)
  }
}
