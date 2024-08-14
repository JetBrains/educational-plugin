package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import training.git.lesson.GitCommitLesson
import training.learn.CourseManager
import training.learn.course.KLesson
import training.statistic.LessonStartingWay

class IFTToolWindowLink(link: String) : TaskDescriptionLink<KLesson, KLesson>(link, EduCounterUsageCollector.LinkType.IFT) {
  override fun resolve(project: Project): KLesson {
    CourseManager.instance.openLesson(project, GitCommitLesson(), LessonStartingWay.LEARN_TAB)
    return GitCommitLesson()
  }

  override suspend fun validate(project: Project, resolved: KLesson): String? {
    TODO("Not yet implemented")
  }

  override fun open(project: Project, resolved: KLesson) {
    TODO("Not yet implemented")
  }
}