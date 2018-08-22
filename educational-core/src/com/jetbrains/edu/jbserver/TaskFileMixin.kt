package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency


abstract class TaskFileMixin {
  @JsonProperty("name") lateinit var name : String
  @JsonProperty("text") lateinit var text : String
  @JsonProperty("placeholders") lateinit var myAnswerPlaceholders : List<AnswerPlaceholder>
}


abstract class AnswerPlaceholderMixin {
  @JsonProperty("offset") var myOffset : Int = -1
  @JsonProperty("length") var myLength : Int = -1
  @JsonProperty("dependency") lateinit var myPlaceholderDependency : AnswerPlaceholderDependency
  @JsonProperty("hints") lateinit var myHints : List<String>
  @JsonProperty("possible_answer") lateinit var myPossibleAnswer : String
  @JsonProperty("placeholder_text") lateinit var myPlaceholderText : String
}


abstract class AnswerPlaceholderDependencyMixin {
  @JsonProperty("section") lateinit var mySectionName : String
  @JsonProperty("lesson") lateinit var myLessonName : String
  @JsonProperty("task") lateinit var myTaskName : String
  @JsonProperty("file") lateinit var myFileName : String
  @JsonProperty("placeholder") var myPlaceholderIndex : Int = -1
}
