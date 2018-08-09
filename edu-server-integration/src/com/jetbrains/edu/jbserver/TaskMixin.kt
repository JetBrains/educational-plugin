package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.annotation.*
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat


@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
@JsonSubTypes(*arrayOf(
  JsonSubTypes.Type(value = TheoryTask::class, name = "theory"),
  JsonSubTypes.Type(value = IdeTask::class, name = "ide"),
  JsonSubTypes.Type(value = OutputTask::class, name = "output"),
  JsonSubTypes.Type(value = EduTask::class, name = "edu"),
  JsonSubTypes.Type(value = CodeTask::class, name = "code"),
  JsonSubTypes.Type(value = ChoiceTask::class, name = "choice")
))
@JsonIgnoreProperties(value = *arrayOf(
  "format", "last_modified", "version_id"
))
abstract class TaskMixIn {

  @JsonProperty("id")
  var myStepId: Int = 0

  @JsonProperty("name")
  lateinit var name: String

  @JsonProperty("description")
  lateinit var descriptionText: String

  @JsonProperty("description_format")
  lateinit var descriptionFormat: DescriptionFormat

  @JsonProperty("task_files")
  lateinit var taskFiles: HashMap<String, TaskFile>

  @JsonProperty("test_files")
  lateinit var testsText: HashMap<String, String>

  @JsonProperty("format")
  lateinit var format: String

}


@JsonTypeName("theory") // M
abstract class TheoryTaskMixIn


@JsonTypeName("ide")
abstract class IdeTaskMixIn


@JsonTypeName("output") // M
abstract class OutputTaskMixIn


@JsonTypeName("edu") // M
abstract class EduTaskMixIn


@JsonTypeName("code")
abstract class CodeTaskMixIn


@JsonTypeName("choice")
abstract class ChoiceTaskMixIn {

  @JsonProperty("choice_variants")
  lateinit var myChoiceVariants: List<String>

  @JsonProperty("is_multichoice")
  var myIsMultipleChoice: Boolean = true

}
