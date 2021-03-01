package com.jetbrains.edu.coursecreator.actions.marketplace

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.CourseArchiveCreator
import com.jetbrains.edu.coursecreator.actions.mixins.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.encrypt.EncryptionModule
import com.jetbrains.edu.learning.encrypt.getAesKey
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.generateCourseItemsIds
import com.jetbrains.edu.learning.marketplace.setRemoteMarketplaceCourseVersion
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle

class MarketplaceArchiveCreator(project: Project, location: String, aesKey: String = getAesKey())
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
    if (course == null) return EduCoreBundle.message("error.unable.to.obtain.course.for.project")
    if (!isUnitTestMode) {
      course.generateCourseItemsIds()
    }
    if (course.marketplaceCourseVersion == 0) {
      course.marketplaceCourseVersion = 1
    }
    course.isMarketplace = true

    if (course.vendor == null) {
      if (!addVendor(course)) return EduCoreBundle.message("marketplace.vendor.empty")
    }

    return super.compute()
  }

  @VisibleForTesting
  fun addVendor(course: Course): Boolean {
    val currentUser = MarketplaceSettings.INSTANCE.account ?: return false
    course.vendor = Vendor(currentUser.userInfo.name)
    return true
  }

  fun createArchiveWithRemoteCourseVersion(): String? {
    if (course == null) return EduCoreBundle.message("error.unable.to.obtain.course.for.project")
    course.setRemoteMarketplaceCourseVersion()
    FileDocumentManager.getInstance().saveAllDocuments()
    return ApplicationManager.getApplication().runWriteAction<String>(this)
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
