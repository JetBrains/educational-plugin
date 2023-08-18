package com.jetbrains.edu.learning.courseFormat.attempts

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.IS_MULTIPLE_CHOICE
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.OPTIONS
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PAIRS

class Dataset {
  @JsonProperty(IS_MULTIPLE_CHOICE)
  var isMultipleChoice: Boolean = false

  @JsonProperty(OPTIONS)
  var options: List<String>? = null

  @JsonProperty(PAIRS)
  var pairs: List<Pair>? = null

  constructor()
  constructor(emptyDataset: String)  // stepik returns empty string instead of null

  class Pair {
    @JsonProperty("first")
    val first: String = ""

    @JsonProperty("second")
    val second: String = ""
  }
}