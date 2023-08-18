package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.ID
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.IS_COMPLETED
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.STEP_ID
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TITLE

class HyperskillStage {

  @Suppress("unused") //used for deserialization
  constructor()

  constructor(stageId: Int, stageTitle: String, stageStepId: Int, isStageCompleted: Boolean = false) {
    id = stageId
    title = stageTitle
    stepId = stageStepId
    isCompleted = isStageCompleted
  }

  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(STEP_ID)
  var stepId: Int = -1

  @JsonProperty(IS_COMPLETED)
  var isCompleted: Boolean = false
}
