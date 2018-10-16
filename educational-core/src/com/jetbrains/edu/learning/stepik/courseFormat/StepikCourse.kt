package com.jetbrains.edu.learning.stepik.courseFormat

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.courseFormat.ext.getTask
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikCourseRemoteInfo
import java.util.*

class StepikCourse : Course() {

  val stepikRemoteInfo: StepikCourseRemoteInfo
    get() {
      val info = super.getRemoteInfo()
      assert(info is StepikCourseRemoteInfo)
      return info as StepikCourseRemoteInfo
    }

  val isCompatible: Boolean get() = (remoteInfo as? StepikCourseRemoteInfo)?.isIdeaCompatible ?: false

  val id: Int get() = (remoteInfo as? StepikCourseRemoteInfo)?.id ?: 0

  var updateDate: Date
    get() = (remoteInfo as? StepikCourseRemoteInfo)?.updateDate ?: Date(0)
    set(date) {
      if (remoteInfo !is StepikCourseRemoteInfo) {
        remoteInfo = StepikCourseRemoteInfo()
      }
      (remoteInfo as StepikCourseRemoteInfo).updateDate = date
    }

  init {
    remoteInfo = StepikCourseRemoteInfo()
  }

  override fun getTags(): List<Tag> {
    val tags = super.getTags()
    if (visibility is CourseVisibility.FeaturedVisibility) {
      tags.add(FeaturedTag())
    }
    if (visibility is CourseVisibility.InProgressVisibility) {
      tags.add(InProgressTag())
    }
    return tags
  }

  fun updateCourseCompatibility() {
    val info = stepikRemoteInfo
    val supportedLanguages = EduConfiguratorManager.supportedLanguages

    val typeLanguage = StringUtil.split(info.courseFormat, " ")
    val prefix = typeLanguage[0]
    if (!supportedLanguages.contains(languageID)) myCompatibility = CourseCompatibility.UNSUPPORTED
    if (typeLanguage.size < 2 || !prefix.startsWith(StepikNames.PYCHARM_PREFIX)) {
      myCompatibility = CourseCompatibility.UNSUPPORTED
      return
    }
    val versionString = prefix.substring(StepikNames.PYCHARM_PREFIX.length)
    if (versionString.isEmpty()) {
      myCompatibility = CourseCompatibility.COMPATIBLE
      return
    }
    myCompatibility = try {
      val version = Integer.valueOf(versionString)
      if (version <= JSON_FORMAT_VERSION) {
        CourseCompatibility.COMPATIBLE
      }
      else {
        CourseCompatibility.INCOMPATIBLE_VERSION
      }
    }
    catch (e: NumberFormatException) {
      LOG.info("Wrong version format", e)
      CourseCompatibility.UNSUPPORTED
    }

  }

  fun getTask(stepId: Int): Task? {
    val taskRef = Ref<Task>()
    course.visitLessons { lesson ->
      val task = lesson.getTask(stepId)
      if (task != null) {
        taskRef.set(task)
        return@visitLessons false
      }
      true
    }
    return taskRef.get()
  }

  companion object {
    private val LOG = Logger.getInstance(Course::class.java)
  }
}
