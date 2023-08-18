package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_TYPE_YAML
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TYPE_YAML
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_TYPE_YAML
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TYPE_YAML
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.errorHandling.unsupportedItemTypeMessage
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.END_DATE_TIME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.GENERATED_EDU_ID
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_PRIVATE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAM_TYPE_ID
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SOLUTIONS_HIDDEN
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUBMIT_MANUALLY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUMMARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TAGS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TITLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VENDOR
import java.time.ZonedDateTime

@Suppress("unused") // used for yaml serialization
@JsonDeserialize(builder = RemoteCourseBuilder::class)
abstract class RemoteCourseYamlMixin : CourseYamlMixin()

@JsonPOJOBuilder(withPrefix = "")
class RemoteCourseBuilder(
  @JsonProperty(TYPE) courseType: String?,
  @JsonProperty(TITLE) title: String,
  @JsonProperty(SUMMARY) summary: String,
  @JsonProperty(VENDOR) yamlVendor: Vendor?,
  @JsonProperty(IS_PRIVATE) yamlIsPrivate: Boolean?,
  @JsonProperty(FEEDBACK_LINK) yamlFeedbackLink: String?,
  @JsonProperty(GENERATED_EDU_ID) yamlGeneratedEduId: String?,
  @JsonProperty(PROGRAMMING_LANGUAGE) displayProgrammingLanguageName: String,
  @JsonProperty(PROGRAMMING_LANGUAGE_VERSION) programmingLanguageVersion: String?,
  @JsonProperty(LANGUAGE) language: String,
  @JsonProperty(ENVIRONMENT) yamlEnvironment: String?,
  @JsonProperty(CONTENT) content: List<String?> = emptyList(),
  @JsonProperty(SUBMIT_MANUALLY) courseraSubmitManually: Boolean?,
  @JsonProperty(SOLUTIONS_HIDDEN) areSolutionsHidden: Boolean?,
  @JsonProperty(END_DATE_TIME) codeforcesEndDateTime: ZonedDateTime?,
  @JsonProperty(PROGRAM_TYPE_ID) codeforcesProgramTypeId: String?,
  @JsonProperty(TAGS) yamlContentTags: List<String> = emptyList(),
) : CourseBuilder(
  courseType,
  title,
  summary,
  yamlVendor,
  yamlIsPrivate,
  yamlFeedbackLink,
  yamlGeneratedEduId,
  displayProgrammingLanguageName,
  programmingLanguageVersion,
  language,
  yamlEnvironment,
  content,
  courseraSubmitManually,
  areSolutionsHidden,
  codeforcesEndDateTime,
  codeforcesProgramTypeId,
  yamlContentTags
) {

  override fun makeCourse(): Course {
    return when (courseType) {
      CourseraNames.COURSE_TYPE_YAML -> {
        CourseraCourse().apply {
          submitManually = courseraSubmitManually ?: false
        }
      }

      CHECKIO_TYPE_YAML -> CheckiOCourse()
      HYPERSKILL_TYPE_YAML -> HyperskillCourse()
      STEPIK_TYPE_YAML -> StepikCourse()
      CODEFORCES_TYPE_YAML -> {
        CodeforcesCourse().apply {
          endDateTime = codeforcesEndDateTime
          programTypeId = codeforcesProgramTypeId
        }
      }
      null -> EduCourse()
      else -> formatError(unsupportedItemTypeMessage(courseType ?: "", EduFormatNames.COURSE))
    }
  }
}

class RemoteEduCourseChangeApplier : RemoteInfoChangeApplierBase<EduCourse>() {
  override fun applyChanges(existingItem: EduCourse, deserializedItem: EduCourse) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.sectionIds = deserializedItem.sectionIds
    existingItem.marketplaceCourseVersion = deserializedItem.marketplaceCourseVersion
    existingItem.generatedEduId = deserializedItem.generatedEduId
  }
}
