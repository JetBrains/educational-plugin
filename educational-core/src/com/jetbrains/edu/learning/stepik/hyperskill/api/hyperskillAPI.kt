package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.stepik.StepSource
import com.jetbrains.edu.learning.stepik.api.REPLY
import com.jetbrains.edu.learning.stepik.api.STEPS

const val PROFILES = "profiles"
const val STAGES = "stages"
const val TOPICS = "topics"
const val PROJECTS = "projects"
const val ID = "id"
const val TITLE = "title"
const val EMAIL = "email"
const val FULL_NAME = "fullname"
const val PROJECT = "project"
const val DESCRIPTION = "description"
const val IDE_FILES = "ide_files"
const val USE_IDE = "use_ide"
const val LANGUAGE = "language"
const val THEORY_ID = "theory"
const val STEP_ID = "step"
const val TOPIC_THEORY = "topic_theory"
const val SOLUTIONS = "solutions"
const val IS_TEMPLATE_BASED = "is_template_based"

class HyperskillAccount : OAuthAccount<HyperskillUserInfo>()

class HyperskillUserInfo {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(EMAIL)
  var email: String = ""

  @JsonProperty(FULL_NAME)
  var fullname: String = ""

  @JsonProperty(PROJECT)
  var hyperskillProjectId: Int? = null

  override fun toString(): String {
    return fullname
  }
}

class HyperskillStage {

  @Suppress("unused") //used for deserialization
  constructor()

  constructor(stageId: Int, stageTitle: String, stageStepId: Int) {
    id = stageId
    title = stageTitle
    stepId = stageStepId
  }

  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(STEP_ID)
  var stepId: Int = -1
}

class HyperskillProject {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(DESCRIPTION)
  var description: String = ""

  @JsonProperty(IDE_FILES)
  var ideFiles: String = ""

  @JsonProperty(USE_IDE)
  var useIde: Boolean = false

  @JsonProperty(LANGUAGE)
  var language: String = ""

  @JsonProperty(IS_TEMPLATE_BASED)
  var isTemplateBased: Boolean = false
}

class HyperskillTopic {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(THEORY_ID)
  var theoryId: Int? = null
}

class HyperskillStepSource : StepSource() {
  @JsonProperty(TITLE)
  var title: String? = null

  @JsonProperty(TOPIC_THEORY)
  var topicTheory: Int? = null
}

class Solution {
  @JsonProperty(ID)
  var id: Int = 0

  @JsonProperty(REPLY)
  lateinit var reply: String
}

// lists

class UsersList {
  @JsonProperty(PROFILES)
  lateinit var profiles: List<HyperskillUserInfo>
}

class StagesList {
  @JsonProperty(STAGES)
  lateinit var stages: List<HyperskillStage>
}

class TopicsList {
  @JsonProperty(TOPICS)
  lateinit var topics: List<HyperskillTopic>
}

class ProjectsList {
  @JsonProperty(PROJECTS)
  lateinit var projects: List<HyperskillProject>
}

class HyperskillStepsList {
  @JsonProperty(STEPS)
  lateinit var steps: List<HyperskillStepSource>
}

class SolutionsList {
  @JsonProperty(SOLUTIONS)
  lateinit var solutions: List<Solution>
}
