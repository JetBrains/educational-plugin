package com.jetbrains.edu.learning.stepik.hyperskill.courseFormat

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.yaml.YamlDeserializationHelper
import com.jetbrains.edu.learning.yaml.YamlDeserializerBase
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.yaml.format.RemoteStudyItem
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames

class HyperskillYamlDeserializer : YamlDeserializerBase() {

  override fun deserializeCourseRemoteInfo(configFileText: String): Course {
    return YamlFormatSynchronizer.REMOTE_MAPPER.readValue(configFileText, HyperskillCourse::class.java)
  }

  override fun deserializeTaskRemoteInfo(configFileText: String): StudyItem {
    val treeNode = YamlFormatSynchronizer.REMOTE_MAPPER.readTree(configFileText)

    val clazz = when (YamlDeserializationHelper.asText(treeNode.get(YamlMixinNames.TYPE))) {
      DataTask.DATA_TASK_TYPE -> DataTask::class.java
      else -> RemoteStudyItem::class.java
    }

    return YamlFormatSynchronizer.REMOTE_MAPPER.treeToValue(treeNode, clazz)
  }
}