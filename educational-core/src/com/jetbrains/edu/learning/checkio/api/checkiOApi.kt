package com.jetbrains.edu.learning.checkio.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.marketplace.api.ID

private const val OBJECTS = "objects"
private const val CODE = "code"
private const val DESCRIPTION = "description"
private const val TITLE = "title"
private const val SECONDS_PAST = "secondsPast"
private const val STATION_ID = "stationId"
private const val STATION_NAME = "stationName"
private const val IS_SOLVED = "isSolved"
private const val SLUG = "slug"

class CheckiOMissionList {
  @JsonProperty(OBJECTS)
  var checkiOMissions: List<CheckiOMissionBean> = emptyList()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CheckiOMissionBean {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(CODE)
  var code: String = ""

  @JsonProperty(DESCRIPTION)
  var description: String = ""

  @JsonProperty(SECONDS_PAST)
  var secondsPast: Long? = null

  @JsonProperty(STATION_ID)
  var stationId: Int = -1

  @JsonProperty(STATION_NAME)
  var stationName: String = ""

  @JsonProperty(IS_SOLVED)
  var isSolved: Boolean = false

  @JsonProperty(SLUG)
  var slug: String = ""
}