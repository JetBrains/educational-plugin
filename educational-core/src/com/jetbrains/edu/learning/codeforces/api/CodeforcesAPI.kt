package com.jetbrains.edu.learning.codeforces.api

import com.fasterxml.jackson.annotation.JsonProperty


open class CodeforcesResponse<T : Any> {
  @JsonProperty(STATUS)
  lateinit var status: String

  @JsonProperty(RESULT)
  lateinit var result: T

  val isOK: Boolean
    get() = status == "OK"

  companion object {
    private const val STATUS = "status"
    private const val RESULT = "result"
  }
}

class ContestsResponse : CodeforcesResponse<List<ContestInfo>>()
class SubmissionsResponse : CodeforcesResponse<List<SubmissionResult>>()
