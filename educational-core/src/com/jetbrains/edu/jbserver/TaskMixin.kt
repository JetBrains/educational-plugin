package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.annotation.*
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.*
import java.util.*


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
@JsonIgnoreProperties(value = *arrayOf("format"))
abstract class TaskMixIn {

  @JsonProperty(ID_FIELD)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var myStepId: Int = 0

  @JsonProperty(VERSION_FIELD)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var versionId: Int = 0

  @JsonProperty(TITLE_FIELD)
  lateinit var name: String

  @JsonProperty(DESCRIPTION_TEXT_FIELD)
  lateinit var descriptionText: String

  @JsonProperty(DESCRIPTION_FORMAT_FIELD)
  lateinit var descriptionFormat: DescriptionFormat

  @JsonProperty(TASK_FILES_FIELD)
  lateinit var taskFiles: HashMap<String, TaskFile>

  @JsonProperty(TEST_FILES_FIELD)
  lateinit var testsText: HashMap<String, String>

  @JsonProperty(LAST_MODIFIED_FIELD)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  lateinit var myUpdateDate: Date

}


@JsonTypeName("theory")
abstract class TheoryTaskMixIn


@JsonTypeName("ide")
abstract class IdeTaskMixIn


@JsonTypeName("output")
abstract class OutputTaskMixIn


@JsonTypeName("edu")
abstract class EduTaskMixIn


@JsonTypeName("code")
abstract class CodeTaskMixIn


@JsonTypeName("choice")
abstract class ChoiceTaskMixIn {

  @JsonProperty(CHOISES_FIELD)
  lateinit var myChoiceVariants: List<String>

  @JsonProperty(IS_MULTICHOICE_FIELD)
  var myIsMultipleChoice: Boolean = true

}
