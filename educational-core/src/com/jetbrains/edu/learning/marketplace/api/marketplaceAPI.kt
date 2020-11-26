package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.courseFormat.EduCourse

const val DATA = "data"
const val DESCRIPTION = "description"
const val DOWNLOADS = "downloads"
const val FIELDS = "fields"
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

class MarketplaceAccount : OAuthAccount<MarketplaceUserInfo>()

class MarketplaceUserInfo {
  @JsonProperty(ID)
  var id: String = ""

  @JsonProperty(NAME)
  var name: String = ""

  @JsonProperty(GUEST)
  var guest: Boolean = false

  @JsonProperty(TYPE)
  var type: String = ""

  override fun toString(): String {
    return name
  }
}

class QueryData(graphqlQuery: String) {
  @JsonProperty(QUERY)
  var query: String = graphqlQuery
}

@JsonIgnoreProperties(ignoreUnknown = true)
class PluginsList {
  @JsonProperty(PLUGINS)
  var courses: List<EduCourse> = emptyList()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class PluginData {
  @JsonProperty(DATA)
  lateinit var data: Plugins
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Plugins {
  @JsonProperty(TOTAL)
  var total: Int = 0

  @JsonProperty(PLUGINS)
  lateinit var plugins: PluginsList
}

class Fields {
  @JsonProperty(PROGRAMMING_LANGUAGE)
  var programmingLanguage: String = ""

  @JsonProperty(LANGUAGE)
  var language: String = ""
}