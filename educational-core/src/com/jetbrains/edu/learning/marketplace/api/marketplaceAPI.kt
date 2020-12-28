package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.UserInfo
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.courseFormat.EduCourse

const val DATA = "data"
const val DESCRIPTION = "description"
const val DEVELOPERS = "developers"
const val DOWNLOADS = "downloads"
const val FIELDS = "fields"
const val COURSE_VERSION = "course_version"
const val GUEST = "guest"
const val ID = "id"
const val LANGUAGE = "language"
const val NAME = "name"
const val PLUGINS = "plugins"
const val PROGRAMMING_LANGUAGE = "programmingLanguage"
const val QUERY = "query"
const val RATING = "rating"
const val TOTAL = "total"
const val TYPE = "type"
const val UPDATES = "updates"

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
  lateinit var updateBean: List<UpdateBean>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UpdateBean {
  @JsonProperty(ID)
  var updateId: Int = -1
}