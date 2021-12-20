package com.jetbrains.edu.learning.codeforces.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss ZZ")

class CodeforcesWSResponse {
  @JsonProperty("id")
  var id: Int = -1

  @JsonProperty("channel")
  var channel: String = ""

  @JsonProperty("text")
  var text: String = ""

}

class DataResponse {
  @JsonProperty("d")
  var data: Array<Any> = emptyArray()

  val verdict: CodeforcesVerdict
    @JsonIgnore
    get() = CodeforcesVerdict.valueOf(data[6] as String)

  val id: Int
    @JsonIgnore
    get() = data[1] as Int

  val date: Date
  @JsonIgnore
  get() = Date.from(ZonedDateTime.parse("${data[13] as String} +0300", formatter).withZoneSameInstant(ZoneId.systemDefault()).toInstant())

  override fun toString(): String {
    return data.joinToString(", ")
  }

}