package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.EduNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.setMarketplaceAuthorsAsString

@JsonDeserialize(builder = MarketplaceCourseBuilder::class)
abstract class MarketplaceEduCourseMixin {

  @JsonProperty(ID)
  var marketplaceId: Int = 0

  @JsonProperty(NAME)
  var myName: String = ""

  @JsonProperty(DESCRIPTION)
  var description: String = ""

  @JsonProperty(DOWNLOADS)
  var learnersCount: Int = 0

  @JsonProperty(RATING)
  var reviewScore: Double? = 0.0

  @JsonProperty(DEVELOPERS)
  var developers: List<String> = emptyList()
}

@JsonPOJOBuilder(withPrefix = "")
private class MarketplaceCourseBuilder(
  @JsonProperty(ID) val courseId: Int,
  @JsonProperty(NAME) val courseName: String,
  @JsonProperty(DESCRIPTION) val courseDescription: String,
  @JsonProperty(DEVELOPERS) val developers: List<String>,
  @JsonProperty(DOWNLOADS) val downloads: Int,
  @JsonProperty(RATING) val rating: Double?,
  @JsonProperty(FIELDS) val fields: Fields,
  @JsonProperty(ORGANIZATION) val courseOrganization: Organization?,
  @JsonProperty(MARKETPLACE_COURSE_VERSION) val version: Int?,
) {
  @Suppress("unused") // used for deserialization
  private fun build(): Course {
    val course = EduCourse()

    course.apply {
      marketplaceId = courseId
      name = courseName
      isMarketplace = true
      description = courseDescription
      learnersCount = downloads
      reviewScore = rating ?: 0.0
      language = fields.programmingLanguage
      languageCode = fields.language
      environment = fields.environment ?: DEFAULT_ENVIRONMENT
      marketplaceCourseVersion = version ?: 1
      organization = courseOrganization?.name
      setMarketplaceAuthorsAsString(developers)
    }

    return course
  }
}
