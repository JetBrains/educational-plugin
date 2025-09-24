package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseVisibility
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.JBAccountUserInfo
import com.jetbrains.edu.learning.marketplace.PLUGINS_REPOSITORY_URL
import com.jetbrains.edu.learning.marketplace.REVIEWS
import java.util.*

private const val AUTHORS = "authors"
private const val CREATE_DATE = "cdate"
private const val DESCRIPTION = "description"
private const val DOWNLOADS = "downloads"
private const val FIELDS = "fields"
private const val LAST_UPDATE_DATE = "lastUpdateDate"
private const val LICENSE = "license"
private const val LINK = "link"
const val MARKETPLACE_COURSE_VERSION = "course_version"
private const val ORGANIZATION = "organization"
private const val RATING = "rating"

@JsonDeserialize(builder = MarketplaceCourseBuilder::class)
abstract class MarketplaceEduCourseMixin

@JsonPOJOBuilder(withPrefix = "")
private class MarketplaceCourseBuilder(
  @param:JsonProperty(ID) val courseId: Int,
  @param:JsonProperty(NAME) val courseName: String,
  @param:JsonProperty(DESCRIPTION) val courseDescription: String,
  @param:JsonProperty(AUTHORS) val marketplaceAuthors: List<Author>,
  @param:JsonProperty(DOWNLOADS) val downloads: Int,
  @param:JsonProperty(RATING) val rating: Double?,
  @param:JsonProperty(FIELDS) val fields: Fields,
  @param:JsonProperty(ORGANIZATION) val courseOrganization: Organization?,
  @param:JsonProperty(MARKETPLACE_COURSE_VERSION) val version: Int?,
  @param:JsonProperty(LAST_UPDATE_DATE) val lastUpdateDate: Long,
  @param:JsonProperty(CREATE_DATE) val courseCreateDate: Long,
  @param:JsonProperty(LINK) val courseLink: String,
  @param:JsonProperty(LICENSE) val courseLicense: String,
) {
  @Suppress("unused") // used for deserialization
  private fun build(): Course {
    val course = EduCourse()

    course.apply {
      id = courseId
      name = courseName
      isMarketplace = true
      description = courseDescription
      learnersCount = downloads
      reviewScore = rating ?: 0.0
      val fieldsLanguageId = fields.languageId
      if (!fieldsLanguageId.isNullOrEmpty()) {
        languageId = fieldsLanguageId
        languageVersion = fields.languageVersion
      } else {
        fields.programmingLanguage.split(" ").apply {
          languageId = first()
          languageVersion = getOrNull(1)
        }
      }
      languageCode = fields.language
      environment = fields.environment ?: DEFAULT_ENVIRONMENT
      marketplaceCourseVersion = version ?: 1
      organization = courseOrganization?.name
      isMarketplacePrivate = fields.isPrivate
      visibility = if (fields.isPrivate) CourseVisibility.PrivateVisibility else CourseVisibility.PublicVisibility
      updateDate = Date(lastUpdateDate)
      createDate = Date(courseCreateDate)
      feedbackLink = "$PLUGINS_REPOSITORY_URL$courseLink$REVIEWS"
      license = courseLicense
      setMarketplaceAuthorsAsString(marketplaceAuthors)
    }

    return course
  }
}

fun Course.setMarketplaceAuthorsAsString(authors: List<Author>) {
  this.authors = authors.map { JBAccountUserInfo(it.name) }
}
