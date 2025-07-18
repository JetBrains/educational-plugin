package com.jetbrains.edu.learning.stepik.hyperskill.update.elements

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.update.elements.CourseUpdate
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.util.*

class HyperskillCourseUpdate(
  override val localItem: HyperskillCourse,
  override val remoteItem: HyperskillCourse
) : CourseUpdate<HyperskillCourse>(localItem, remoteItem) {
  override suspend fun update(project: Project) {
    baseUpdate(project)

    val remoteProject = remoteItem.hyperskillProject ?: error("'hyperskillProject' is not initialized")
    localItem.name = remoteProject.title
    localItem.description = remoteProject.description
    localItem.hyperskillProject = remoteProject

    localItem.stages = remoteItem.stages
    localItem.taskToTopics = remoteItem.taskToTopics

    localItem.updateDate = Date()
    localItem.environment = remoteItem.environment

    YamlFormatSynchronizer.saveItemWithRemoteInfo(localItem)
  }
}