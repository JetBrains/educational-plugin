package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.EduNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.UserInfo
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.courseFormat.EduCourse

const val DATA = "data"
const val DESCRIPTION = "description"
const val DEVELOPERS = "developers"
const val DOWNLOADS = "downloads"
const val ENVIRONMENT = "environment"
const val FIELDS = "fields"
const val GUEST = "guest"
const val ID = "id"
const val IS_PRIVATE = "isPrivate"
const val LANGUAGE = "language"
const val LAST_UPDATE_DATE = "lastUpdateDate"
const val MARKETPLACE_COURSE_VERSION = "course_version"
const val NAME = "name"
const val ORGANIZATION = "organization"
const val PLUGINS = "plugins"
const val PROGRAMMING_LANGUAGE = "programmingLanguage"
const val QUERY = "query"
const val RATING = "rating"
const val TOTAL = "total"
const val TYPE = "type"
const val UPDATES = "updates"
const val VERSION = "version"

class MarketplaceAccount : OAuthAccount<MarketplaceUserInfo>()

class MarketplaceUserInfo() : UserInfo {
  @JsonProperty(ID)
  var id: String = ""

  @JsonProperty(NAME)
  var name: String = ""

  @JsonProperty(GUEST)
  var guest: Boolean = false

  @JsonProperty(TYPE)
  var type: String = ""

  constructor(userName: String) : this() {
    name = userName
  }

  override fun getFullName(): String = name

  override fun toString(): String {
    return name
  }
}

class QueryData(graphqlQuery: String) {
  @JsonProperty(QUERY)
  var query: String = graphqlQuery
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CoursesList {
  @JsonProperty(TOTAL)
  var total: Int = -1

  @JsonProperty(PLUGINS)
  var courses: List<EduCourse> = emptyList()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CoursesData {
  @JsonProperty(DATA)
  lateinit var data: Courses
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Courses {

  @JsonProperty(PLUGINS)
  lateinit var coursesList: CoursesList
}

class Fields {
  @JsonProperty(PROGRAMMING_LANGUAGE)
  var programmingLanguage: String = ""

  @JsonProperty(LANGUAGE)
  var language: String = ""

  @JsonProperty(ENVIRONMENT)
  var environment: String? = DEFAULT_ENVIRONMENT

  @JsonProperty(IS_PRIVATE)
  var isPrivate: Boolean = false
}

class Organization {
  @JsonProperty(NAME)
  var name: String? = ""
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UpdateData {
  @JsonProperty(DATA)
  lateinit var data: Updates
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Updates {
  @JsonProperty(TOTAL)
  var total: Int = 0

  @JsonProperty(UPDATES)
  lateinit var updates: UpdatesList
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UpdatesList {
  @JsonProperty(UPDATES)
  lateinit var updateInfoList: List<UpdateInfo>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UpdateInfo {
  @JsonProperty(ID)
  var updateId: Int = -1

  @JsonProperty(VERSION)
  var version: Int = -1
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CourseBean {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(NAME)
  var name: String = ""
}
