package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.stepik.StepikNames
import org.jetbrains.annotations.NonNls
import java.util.*

/**
 * Used in the following Mixins:
 * - [com.jetbrains.edu.coursecreator.actions.mixins.RemoteEduCourseMixin]
 * - [com.jetbrains.edu.learning.marketplace.api.MarketplaceEduCourseMixin]
 * - [com.jetbrains.edu.learning.stepik.api.StepikEduCourseMixin]
 * - [com.jetbrains.edu.learning.yaml.format.EduCourseRemoteInfoYamlMixin]
 * - [com.jetbrains.edu.learning.yaml.format.CourseYamlMixin]
 */
open class EduCourse : Course() {
  //course type in format "pycharm<version> <language> <version>$ENVIRONMENT_SEPARATOR<environment>"
  @Suppress("LeakingThis") // TODO[ktisha]: remove `type` once we move all courses to the marketplace
  var type: String = "${StepikNames.PYCHARM_PREFIX}$JSON_FORMAT_VERSION $programmingLanguage"
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
  var reviewSummary: Int = 0

  override var programmingLanguage: String
    get() = super.programmingLanguage
    set(value) {
      super.programmingLanguage = value
      updateType(value)
    }

  override val itemType: String
    get() = if (isMarketplace) MARKETPLACE else super.itemType

  private fun updateType(language: String) {
    type = if (environment != DEFAULT_ENVIRONMENT) {
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

  override val isStepikRemote: Boolean
    get() = id != 0 && !isMarketplace

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
    type = "${StepikNames.PYCHARM_PREFIX}$JSON_FORMAT_VERSION $programmingLanguage"
    id = 0
    updateDate = Date(0)
  }

  override val isViewAsEducatorEnabled: Boolean
    get() = super.isViewAsEducatorEnabled &&
            dataHolder.getUserData(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW_KEY) != true

  companion object {
    @NonNls
    const val ENVIRONMENT_SEPARATOR = "#"
  }
}
