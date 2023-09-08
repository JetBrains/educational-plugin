package com.jetbrains.edu.learning.courseFormat.attempts

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COLUMNS
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.IS_CHECKBOX
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.IS_MULTIPLE_CHOICE
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.OPTIONS
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PAIRS
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.ROWS

@Suppress("unused", "UNUSED_PARAMETER")
class Dataset {
  @JsonProperty(IS_MULTIPLE_CHOICE)
  var isMultipleChoice: Boolean = false

  @JsonProperty(OPTIONS)
  var options: List<String>? = null

  @JsonProperty(PAIRS)
  var pairs: List<Pair>? = null

  @JsonProperty(ROWS)
  var rows: List<String>? = null

  @JsonProperty(COLUMNS)
  var columns: List<String>? = null

  @JsonProperty(IS_CHECKBOX)
  var isCheckbox: Boolean = false

  constructor()
  constructor(emptyDataset: String)  // stepik returns empty string instead of null

  class Pair {
    @JsonProperty("first")
    val first: String = ""

    @JsonProperty("second")
    val second: String = ""
  }
}
