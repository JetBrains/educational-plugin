package com.jetbrains.edu.learning.codeforces.courseFormat

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.YamlDeserializationHelper
import com.jetbrains.edu.learning.yaml.YamlDeserializerBase
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.errorHandling.unsupportedItemTypeMessage
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames

class CodeforcesYamlDeserializer : YamlDeserializerBase() {
  override fun deserializeCourseRemoteInfo(configFileText: String): Course {
    return YamlFormatSynchronizer.REMOTE_MAPPER.readValue(configFileText, CodeforcesCourse::class.java)
  }

  override fun deserializeTask(mapper: ObjectMapper, configFileText: String): Task {
    val treeNode = mapper.readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()
    val type = YamlDeserializationHelper.asText(treeNode.get(YamlMixinNames.TYPE)) ?: formatError(
      EduCoreBundle.message("yaml.editor.invalid.task.type.not.specified"))

    val clazz = when (type) {
      CodeforcesNames.CODEFORCES_TASK_TYPE -> CodeforcesTask::class.java
      CodeforcesNames.CODEFORCES_TASK_TYPE_WITH_FILE_IO -> CodeforcesTaskWithFileIO::class.java
      else -> formatError(unsupportedItemTypeMessage(type, EduNames.TASK))
    }

    return mapper.treeToValue(treeNode, clazz)
  }
}