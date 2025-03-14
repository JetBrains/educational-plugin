package com.jetbrains.edu.decomposition.test

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the generated test for the specific task and keeps it in a State component avoiding generating more than once
 */
@Service(Service.Level.PROJECT)
@State(name = "TestManager", storages = [Storage("storage.xml")])
class TestDependenciesManager : PersistentStateComponent<TestDependenciesManager> {
  private val tests = mutableMapOf<Int, Map<String, List<String>>>()
  private val testGenerationSignals = ConcurrentHashMap<Int, CompletableDeferred<Unit>>()
  private val activeWaitingJobs = ConcurrentHashMap<Int, Job>()

  suspend fun waitForTestGeneration(taskId: Int, functionNames: List<String>) {
    activeWaitingJobs[taskId]?.cancel()
    testGenerationSignals[taskId] = CompletableDeferred()
    coroutineScope {
      val job = launch {
        val deferred = testGenerationSignals.getOrPut(taskId) { CompletableDeferred() }
        while (!isTestGenerated(taskId, functionNames)) {
          deferred.await()
        }
      }
      activeWaitingJobs[taskId] = job
      job.join()
    }
  }

  fun addTest(taskId: Int, dependencies: Map<String, List<String>>) {
    tests[taskId] = dependencies
    testGenerationSignals[taskId]?.complete(Unit)
  }

  fun getTest(taskId: Int) = tests[taskId]

  override fun getState() = this

  override fun loadState(state: TestDependenciesManager) {
    tests.putAll(state.tests)
  }

  fun isTestGenerated(taskId: Int, functionNames: List<String>): Boolean {
    return tests[taskId]?.keys?.toSet()?.equals(functionNames.toSet()) ?: false
  }

  companion object {
    fun getInstance(project: Project): TestDependenciesManager = project.service<TestDependenciesManager>()
  }
}