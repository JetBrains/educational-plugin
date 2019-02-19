package com.jetbrains.edu.learning.stepik.hyperskill

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.authUtils.OAuthAccount

const val PROFILES = "profiles"
const val STAGES = "stages"
const val TOPICS = "topics"
const val PROJECTS = "projects"
const val ID = "id"
const val TITLE = "title"
const val EMAIL = "email"
const val FULL_NAME = "fullname"
const val STAGE = "stage"
const val PROJECT = "project"
const val DESCRIPTION = "description"
const val IDE_FILES = "ide_files"
const val USE_IDE = "use_ide"
const val THEORY_ID = "theory"
const val STEP_ID = "step"

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
}

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

class HyperskillTopic {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(THEORY_ID)
  var theoryId: Int? = null
}