package com.jetbrains.edu.learning.yaml

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSE
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LESSON
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SECTION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TASK
import org.jetbrains.annotations.NonNls

object YamlConfigSettings {
  val COURSE_CONFIG = getLocalConfigFileName(COURSE)
  val SECTION_CONFIG = getLocalConfigFileName(SECTION)
  val LESSON_CONFIG = getLocalConfigFileName(LESSON)
  val TASK_CONFIG = getLocalConfigFileName(TASK)

  /**
   * @param itemKind Course/Section/Lesson/Task
   */
  fun getLocalConfigFileName(itemKind: String): String = "$itemKind-info.yaml"

  @NonNls
  const val REMOTE_COURSE_CONFIG = "course-remote-info.yaml"

  @NonNls
  const val REMOTE_SECTION_CONFIG = "section-remote-info.yaml"

  @NonNls
  const val REMOTE_LESSON_CONFIG = "lesson-remote-info.yaml"

  @NonNls
  const val REMOTE_TASK_CONFIG = "task-remote-info.yaml"

  val StudyItem.remoteConfigFileName: String
    get() = when (this) {
      is Course -> REMOTE_COURSE_CONFIG
      is Section -> REMOTE_SECTION_CONFIG
      is Lesson -> REMOTE_LESSON_CONFIG
      is Task -> REMOTE_TASK_CONFIG
      else -> {
        @NonNls
        val errorMessageToLog = "Unknown StudyItem type: ${javaClass.simpleName}"
        error(errorMessageToLog)
      }
    }
}
