package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.dataSource.LocalDataSource
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.courseFormat.tasks.Task

@Service(Service.Level.PROJECT)
@State(name = "SqlInitializationState", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class SqlInitializationState(private val project: Project) : SimplePersistentStateComponent<SqlInitializationState.State>(State()) {

  init {
    project.messageBus.connect().subscribe(DATA_SOURCE_STORAGE_TOPIC, object : DataSourceStorageListener {
      override fun dataSourceRemoved(dataSource: LocalDataSource) {
        val url = dataSource.url ?: return
        state.removeInitializedDatabase(url)
      }
    })
  }

  var dataSourceInitialized: Boolean by state::dataSourceInitialized

  fun taskDatabaseInitialized(task: Task) {
    val url = task.databaseUrl(project) ?: return
    state.addInitializedDatabase(url)
  }

  fun isTaskDatabaseInitialized(task: Task): Boolean {
    val url = task.databaseUrl(project)
    return url in state.taskDatabasesInitialized
  }

  companion object {
    fun getInstance(project: Project): SqlInitializationState = project.service()
  }

  class State : BaseState() {
    var dataSourceInitialized: Boolean by property(false)
    @get:XCollection(style = XCollection.Style.v2)
    val taskDatabasesInitialized: MutableSet<String> by stringSet()

    fun addInitializedDatabase(url: String) {
      taskDatabasesInitialized += url
      incrementModificationCount()
    }

    fun removeInitializedDatabase(url: String) {
      taskDatabasesInitialized -= url
      incrementModificationCount()
    }
  }
}
