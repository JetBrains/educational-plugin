package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.CourseVisibility.FeaturedVisibility
import com.jetbrains.edu.learning.courseFormat.CourseVisibility.InProgressVisibility
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.stepik.StepikNames
import org.jetbrains.annotations.NonNls
import java.util.*

open class EduCourse : Course() {
  //course type in format "pycharm<version> <language> <version>$ENVIRONMENT_SEPARATOR<environment>"
  var type: String = "${StepikNames.PYCHARM_PREFIX}$JSON_FORMAT_VERSION $language"
  @Transient
  var isUpToDate: Boolean = true
  var learnersCount: Int = 0
  var reviewScore: Double = 0.0

  // Fields that should be moved to StepikCourse:
  var isCompatible: Boolean = true
  var isAdaptive: Boolean = false

  // in CC mode is used to store top-level lessons' section id
  var sectionIds: List<Int> = emptyList()
  var instructors: List<Int> = emptyList()
  var isStepikPublic: Boolean = false
  @Transient
  var adminsGroup: String? = null
  var reviewSummary: Int = 0

  override fun setLanguage(language: String) {
    super.setLanguage(language)
    updateType(language)
  }

  override fun getTags(): List<Tag> {
    val tags = super.getTags()
    if (visibility is FeaturedVisibility) {
      tags.add(FeaturedTag())
    }
    if (visibility is InProgressVisibility) {
      tags.add(InProgressTag())
    }
    return tags
  }

  override fun getItemType(): String {
    return if (isMarketplace) MARKETPLACE else super.getItemType()
  }

  override fun getId(): Int {
    return myId
  }

  private fun updateType(language: String) {
    type = if (environment != EduNames.DEFAULT_ENVIRONMENT) {
      "${StepikNames.PYCHARM_PREFIX}$formatVersion $language$ENVIRONMENT_SEPARATOR$environment"
    }
    else {
      "${StepikNames.PYCHARM_PREFIX}$formatVersion $language"
    }
  }

  val formatVersion: Int
    get() {
      val languageSeparator = type.indexOf(" ")
      if (languageSeparator != -1 && type.contains(StepikNames.PYCHARM_PREFIX)) {
        val formatVersion = type.substring(StepikNames.PYCHARM_PREFIX.length, languageSeparator)
        return try {
          formatVersion.toInt()
        }
        catch (e: NumberFormatException) {
          JSON_FORMAT_VERSION
        }
      }
      return JSON_FORMAT_VERSION
    }

  override fun isStepikRemote(): Boolean {
    return id != 0 && !isMarketplace
  }

  val isMarketplaceRemote: Boolean
    get() = id != 0 && isMarketplace

  fun convertToLocal() {
    if (isMarketplace) {
      marketplaceCourseVersion = 1
    }
    else {
      isStepikPublic = false
      isCompatible = true
      sectionIds = emptyList()
      instructors = emptyList()
    }
    type = "${StepikNames.PYCHARM_PREFIX}$JSON_FORMAT_VERSION $language"
    id = 0
    updateDate = Date(0)
  }

  override fun isViewAsEducatorEnabled(): Boolean {
    return super.isViewAsEducatorEnabled() && dataHolder.getUserData(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW_KEY) != true
  }

  companion object {
    @NonNls
    const val ENVIRONMENT_SEPARATOR = "#"
  }
}
