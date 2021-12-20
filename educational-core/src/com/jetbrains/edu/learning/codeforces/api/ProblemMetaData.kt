package com.jetbrains.edu.learning.codeforces.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class ProblemMetaData {
  @JsonProperty("contestId")
  var id: Int = -1

  @JsonProperty("index")
  var index: String = ""

  @JsonProperty("name")
  var name: String = ""

  @JsonIgnore
  var submissionResult: SubmissionResult? = null
}