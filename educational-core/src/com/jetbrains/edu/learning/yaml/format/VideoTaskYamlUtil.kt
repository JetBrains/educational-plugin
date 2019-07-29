package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.courseFormat.tasks.VideoSource
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CURRENT_TIME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LABEL
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.RECORD
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.RES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SOURCES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SRC
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.THUMBNAIL
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VIDEO_TYPE
import com.jetbrains.edu.learning.yaml.format.student.StudentTaskYamlMixin

@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, THUMBNAIL, SOURCES, CURRENT_TIME, STATUS, RECORD)
abstract class VideoTaskYamlMixin : StudentTaskYamlMixin() {
  @JsonProperty(THUMBNAIL)
  private var thumbnail: String = ""

  @JsonProperty(SOURCES)
  private lateinit var sources: List<VideoSource>

  @JsonProperty(CURRENT_TIME)
  private var currentTime = 0
}

@JsonPropertyOrder(SRC, RES, VIDEO_TYPE, LABEL)
abstract class VideoSourceYamlMixin {
  @JsonProperty(SRC)
  private var src = ""

  @JsonProperty(RES)
  private var res = ""

  @JsonProperty(VIDEO_TYPE)
  private var type = ""

  @JsonProperty(LABEL)
  private var label = ""
}