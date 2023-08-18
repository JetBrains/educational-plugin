package com.jetbrains.edu.learning.courseFormat.hyperskill

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.ID
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.THEORY_ID
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TITLE

class HyperskillTopic {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(THEORY_ID)
  var theoryId: Int? = null
}