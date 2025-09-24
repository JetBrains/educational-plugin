package com.jetbrains.edu.learning.marketplace.courseStorage.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseVisibility
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT

private const val ID = "courseId"
private const val TITLE = "title"
private const val SUMMARY = "summary"
private const val UPDATE_VERSION = "updateVersion"
private const val FORMAT_VERSION = "formatVersion"
private const val PROGRAMMING_LANGUAGE_ID = "programmingLanguageId"
private const val PROGRAMMING_LANGUAGE_VERSION = "programmingLanguageVersion"
private const val LANGUAGE = "language"
private const val ENVIRONMENT = "environment"

@JsonDeserialize(builder = CourseStorageCourseBuilder::class)
abstract class CourseStorageEduCourseMixin

@JsonPOJOBuilder(withPrefix = "")
private class CourseStorageCourseBuilder(
  @param:JsonProperty(ID) val courseId: Int,
  @param:JsonProperty(TITLE) val courseName: String,
  @param:JsonProperty(SUMMARY) val courseDescription: String,
  @param:JsonProperty(UPDATE_VERSION) val updateVersion: Int,
  @param:JsonProperty(FORMAT_VERSION) val remoteFormatVersion: Int,
  @param:JsonProperty(PROGRAMMING_LANGUAGE_ID) val programmingLanguageId: String,
  @param:JsonProperty(PROGRAMMING_LANGUAGE_VERSION) val programmingLanguageVersion: String?,
  @param:JsonProperty(LANGUAGE) val language: String,
  @param:JsonProperty(ENVIRONMENT) val env: String?,
) {
  @Suppress("unused") // used for deserialization
  private fun build(): Course {
    val course = EduCourse()

    course.apply {
      id = courseId
      name = courseName
      isMarketplace = true
      description = courseDescription
      languageId = programmingLanguageId
      languageVersion = programmingLanguageVersion
      languageCode = language
      environment = env ?: DEFAULT_ENVIRONMENT
      marketplaceCourseVersion = updateVersion
      formatVersion = remoteFormatVersion
      visibility = CourseVisibility.FeaturedVisibility(0)
    }

    return course
  }
}