package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.*
import com.jetbrains.edu.learning.courseFormat.CheckFeedback
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.json.mixins.NotImplementedInMixin
import com.jetbrains.edu.learning.yaml.format.tasks.TaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ACTUAL
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.EXPECTED
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.MESSAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.RECORD
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TAGS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TIME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import java.util.*

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, STATUS, FEEDBACK, RECORD, TAGS)
abstract class StudentTaskYamlMixin : TaskYamlMixin() {

  protected var checkStatus: CheckStatus
    @JsonGetter(STATUS)
    get() = throw NotImplementedInMixin()
    @JsonSetter(STATUS)
    set(value) { throw NotImplementedInMixin() }

  @JsonProperty(FEEDBACK)
  private lateinit var feedback: CheckFeedback

  @JsonProperty(RECORD)
  protected open var record: Int = -1
}

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(MESSAGE, TIME, EXPECTED, ACTUAL)
abstract class FeedbackYamlMixin {
  @JsonProperty(MESSAGE)
  private var message: String = ""

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, dd MMM yyyy HH:mm:ss zzz")
  @JsonProperty(TIME)
  private var time: Date? = null

  @JsonProperty(EXPECTED)
  private var expected: String? = null

  @JsonProperty(ACTUAL)
  private var actual: String? = null
}

