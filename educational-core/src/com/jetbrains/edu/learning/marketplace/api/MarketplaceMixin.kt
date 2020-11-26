package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse

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
}

@JsonPOJOBuilder(withPrefix = "")
private class MarketplaceCourseBuilder(
  @JsonProperty(ID) val courseId: Int,
  @JsonProperty(NAME) val courseName: String,
  @JsonProperty(DESCRIPTION) val courseDescription: String,
  @JsonProperty(DOWNLOADS) val downloads: Int,
  @JsonProperty(RATING) val rating: Double,
  @JsonProperty(FIELDS) val fields: Fields,
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
    }

    return course
  }
}

