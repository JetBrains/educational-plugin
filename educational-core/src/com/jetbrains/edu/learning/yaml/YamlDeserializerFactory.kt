package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOYamlDeserializer
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesYamlDeserializer
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.course.StepikYamlDeserializer
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TYPE
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillYamlDeserializer
import com.jetbrains.edu.learning.yaml.YamlDeserializationHelper.getConfigFileForChild

object YamlDeserializerFactory {
  fun getDeserializer(courseType: String): YamlDeserializer {
    return when (courseType) {
      StepikNames.STEPIK_TYPE -> StepikYamlDeserializer()
      HYPERSKILL_TYPE -> HyperskillYamlDeserializer()
      CheckiONames.CHECKIO_TYPE -> CheckiOYamlDeserializer()
      CodeforcesNames.CODEFORCES_COURSE_TYPE -> CodeforcesYamlDeserializer()
      else -> getDefaultDeserializer()
    }
  }

  fun getDeserializer(course: Course?): YamlDeserializer {
    val courseType = course?.itemType ?: ""
    return getDeserializer(courseType)
  }

  fun getDefaultDeserializer(): YamlDeserializerBase {
    return YamlDeserializerBase()
  }
}

abstract class YamlDeserializer {
  abstract fun deserializeItem(configFile: VirtualFile, project: Project?, mapper: ObjectMapper = YamlFormatSynchronizer.MAPPER): StudyItem?

  @VisibleForTesting
  abstract fun deserializeCourse(mapper: ObjectMapper, configFileText: String): Course

  @VisibleForTesting
  abstract fun deserializeSection(mapper: ObjectMapper, configFileText: String): Section

  @VisibleForTesting
  abstract fun deserializeLesson(mapper: ObjectMapper, configFileText: String): Lesson

  @VisibleForTesting
  abstract fun deserializeTask(mapper: ObjectMapper, configFileText: String): Task

  inline fun <reified T : StudyItem> deserializeContent(
    project: Project,
    parentItem: T,
    contentList: List<T>,
    mapper: ObjectMapper = YamlFormatSynchronizer.MAPPER,
  ): List<T> {
    val content = mutableListOf<T>()
    for (titledItem in contentList) {
      val configFile: VirtualFile = parentItem.getConfigFileForChild(project, titledItem.name) ?: continue
      val deserializeItem = deserializeItem(configFile, project, mapper) as? T ?: continue
      deserializeItem.name = titledItem.name
      deserializeItem.index = titledItem.index
      content.add(deserializeItem)
    }

    return content
  }

  abstract fun deserializeRemoteItem(configFile: VirtualFile): StudyItem
}