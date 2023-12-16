package com.jetbrains.edu.learning.codeforces.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class SubmissionResult {
  @JsonProperty("contestId")
  var contestId: Int = -1

  @JsonProperty("id")
  var id: Int = -1

  @JsonProperty("problem")
  lateinit var problem: ProblemMetaData

  @JsonProperty("creationTimeSeconds")
  var creationTimeSeconds: Int = -1

  @JsonProperty("verdict")
  lateinit var verdictString: String

  val verdict: CodeforcesVerdict
    @JsonIgnore
    get() = CodeforcesVerdict.valueOf(verdictString)
}

class SubmissionSource {
  @JsonProperty("source")
  var source = ""
}
