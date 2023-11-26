package com.jetbrains.edu.fleet.common.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.YamlDeserializer
import com.jetbrains.edu.learning.yaml.YamlDeserializer.childrenConfigFileNames
import com.jetbrains.edu.learning.yaml.YamlMapper
import com.jetbrains.edu.learning.yaml.errorHandling.noDirForItemMessage
import fleet.api.*
import fleet.common.fs.fsService
import com.jetbrains.edu.fleet.common.format.getDir
import com.jetbrains.edu.fleet.common.yaml.YamlFormatSynchronizer.mapper

object YamlDeepLoader {
  suspend fun loadCourse(courseDir: FileAddress): Course? {
    val fsApi = requireNotNull(fsService(courseDir)) { "There must be fs service for file $courseDir" }

    val courseConfig = courseDir.child(YamlConfigSettings.COURSE_CONFIG)
    if (!fsApi.exists(courseConfig.path)) error("Course yaml config cannot be null")
    val fileText = fsApi.readFileAndUnwrap(courseConfig.path).decodeToString()

    val deserializedCourse = YamlDeserializer.deserializeItem(courseConfig.name, YamlMapper.MAPPER, fileText) as? Course ?: return null
    val mapper = deserializedCourse.mapper

    deserializedCourse.items = deserializedCourse.deserializeContent(courseDir, deserializedCourse.items, mapper)
    deserializedCourse.items.forEach { deserializedItem ->
      when (deserializedItem) {
        is Section -> {
          // set parent to correctly obtain dirs in deserializeContent method
          deserializedItem.items = deserializedItem.deserializeContent(courseDir, deserializedItem.items, mapper)
          deserializedItem.lessons.forEach {
            it.parent = deserializedItem
            it.items = it.deserializeContent(courseDir, it.taskList, mapper)
          }
        }
        is Lesson -> {
          // set parent to correctly obtain dirs in deserializeContent method
          deserializedItem.parent = deserializedCourse
          deserializedItem.items = deserializedItem.deserializeContent(courseDir, deserializedItem.taskList, mapper)
        }
      }
    }

    // we init course before setting description and remote info, as we have to set parent item
    // to obtain description/remote config file to set info from
    deserializedCourse.init( false)

    return deserializedCourse
  }

  private suspend inline fun <reified T : StudyItem> StudyItem.deserializeContent(courseDir: FileAddress,
                                                                                  contentList: List<T>,
                                                                                  mapper: ObjectMapper = YamlMapper.MAPPER): List<T> {
    val fsApi = requireNotNull(fsService(courseDir)) { "There must be fs service for file $courseDir" }
    val content = mutableListOf<T>()
    for (titledItem in contentList) {
      val configFile = getConfigFileForChild(courseDir, titledItem.name) ?: continue
      val fileText = fsApi.readFileAndUnwrap(configFile.path).decodeToString()
      val deserializeItem = YamlDeserializer.deserializeItem(configFile.name, mapper, fileText) as? T ?: continue
      deserializeItem.name = titledItem.name
      deserializeItem.index = titledItem.index
      content.add(deserializeItem)
    }

    return content
  }

  private suspend fun StudyItem.getConfigFileForChild(courseDir: FileAddress, childName: String): FileAddress? {
    val fsApi = requireNotNull(fsService(courseDir)) { "There must be fs service for file $courseDir" }
    val dir = getDir(courseDir) ?: error(noDirForItemMessage(name))
    val itemDir = dir.child(childName)
    return childrenConfigFileNames.map { itemDir.child(it) }.firstOrNull { fsApi.exists(it.path) }
  }

}