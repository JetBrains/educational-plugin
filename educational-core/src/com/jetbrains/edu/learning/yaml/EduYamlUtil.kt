package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.yaml.YamlDeepLoader
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.addMixIns
import com.jetbrains.edu.coursecreator.yaml.format.TaskYamlMixin
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask

object EduYamlUtil {

  @JvmStatic
  val EDU_MAPPER: ObjectMapper by lazy {
    val mapper = YamlFormatSynchronizer.createMapper()
    addMixIns(mapper)
    mapper.resetEduMixins()

    mapper
  }

  private fun ObjectMapper.resetEduMixins() {
    addMixIn(Task::class.java, TaskYamlMixin::class.java)
    addMixIn(EduTask::class.java, EduTaskYamlMixin::class.java)
    addMixIn(ChoiceTask::class.java, EduChoiceTaskYamlMixin::class.java)
    addMixIn(TaskFile::class.java, EduTaskFileYamlMixin::class.java)
    addMixIn(AnswerPlaceholder::class.java, EduAnswerPlaceholderYamlMixin::class.java)
    addMixIn(AnswerPlaceholder.MyInitialState::class.java, InitialStateMixin::class.java)
  }

  @JvmStatic
  fun saveAll(project: Project) {
    YamlFormatSynchronizer.saveAll(project, EDU_MAPPER)
  }

  @JvmStatic
  fun saveItem(item: StudyItem) {
    YamlFormatSynchronizer.saveItem(item, EDU_MAPPER)
  }

  @JvmStatic
  fun loadCourse(project: Project) = YamlDeepLoader.loadCourse(project, EDU_MAPPER)
}