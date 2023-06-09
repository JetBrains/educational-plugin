package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseUpdateListener
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.sql.core.SqlConfiguratorBase

class SqlCourseUpdateListener : CourseUpdateListener {
  override fun courseUpdated(project: Project, course: Course) {
    if (course.configurator !is SqlConfiguratorBase) return

    val dataSourceManager = LocalDataSourceManager.getInstance(project)

    for (dataSource in dataSourceManager.dataSources) {
      if (dataSource.isTaskDataSource() && dataSource.task(project) == null) {
        dataSourceManager.removeDataSource(dataSource)
      }
    }

    val dataSourceUrls = dataSourceManager.dataSources.mapNotNullTo(HashSet()) { it.url }

    val tasksWithoutDataSource = course.allTasks.filter {
        it.databaseUrl(project) !in dataSourceUrls
    }

    createDataSources(project, tasksWithoutDataSource)
    attachSqlConsoleForOpenFiles(project)
    executeInitScripts(project, tasksWithoutDataSource)
  }
}
