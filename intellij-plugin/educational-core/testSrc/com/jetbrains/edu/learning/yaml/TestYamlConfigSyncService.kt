package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.EduTestAware
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.asCompletableFuture

class TestYamlConfigSyncService(project: Project, scope: CoroutineScope) : YamlConfigSyncServiceImpl(project, scope), EduTestAware {
  fun waitForAllJobs() {
    item2SaveJob.values.forEach {
      PlatformTestUtil.waitForFuture(it.asCompletableFuture())
    }

    if (ApplicationManager.getApplication().isDispatchThread) {
      // This waits for load jobs before the loading of the YAML configs is moved to this service
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
  }

  override fun cleanUpState() {
    waitForAllJobs()
  }

  companion object {
    fun getInstance(project: Project): TestYamlConfigSyncService = YamlConfigSyncService.getInstance(project) as TestYamlConfigSyncService
  }
}
