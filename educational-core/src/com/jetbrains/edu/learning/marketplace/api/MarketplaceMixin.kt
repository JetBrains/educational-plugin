package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Vendor

@JsonDeserialize(builder = MarketplaceCourseBuilder::class)
abstract class MarketplaceEduCourseMixin {

  @JsonProperty(ID)
  var myId: Int = 0

  @JsonProperty(NAME)
  var myName: String = ""

  @JsonProperty(DESCRIPTION)
  var description: String = ""

  @JsonProperty(DOWNLOADS)
  var learnersCount: Int = 0

  @JsonProperty(RATING)
  var reviewScore: Double = 0.0

  @JsonProperty(ORGANIZATION)
  var organization: String = ""
}

@JsonPOJOBuilder(withPrefix = "")
private class MarketplaceCourseBuilder(
  @JsonProperty(ID) val courseId: Int,
  @JsonProperty(NAME) val courseName: String,
  @JsonProperty(DESCRIPTION) val courseDescription: String,
  @JsonProperty(DOWNLOADS) val downloads: Int,
  @JsonProperty(RATING) val rating: Double,
  @JsonProperty(ORGANIZATION) val organization: String,
  @JsonProperty(FIELDS) val fields: Fields,
  @JsonProperty(COURSE_VERSION) val version: Int,
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
      reviewScore = rating
      //marketplace returns programming language in upper case
      language = fields.programmingLanguage.toLowerCase().capitalize()
      languageCode = fields.language
      vendor = Vendor(organization)
      courseVersion = version
    }

    return course
  }
}

