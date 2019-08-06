package com.jetbrains.edu.learning.codeforces.api

import com.fasterxml.jackson.annotation.JsonProperty


class ContestsList {
  @JsonProperty(STATUS)
  lateinit var status: String

  @JsonProperty(RESULT)
  lateinit var contests: List<ContestInfo>

  val isOK: Boolean
    get() = status == "OK"

  companion object {
    private const val STATUS = "status"
    private const val RESULT = "result"
  }
}
