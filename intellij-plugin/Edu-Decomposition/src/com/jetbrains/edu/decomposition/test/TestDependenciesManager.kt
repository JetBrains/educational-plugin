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
  private val testStates = ConcurrentHashMap<Int, TestState>()

  private sealed class TestState(val expectedFunctionNames: Set<String>) {
    class Pending(expectedFunctionNames: Set<String>, val signal: CompletableDeferred<Unit>) : TestState(expectedFunctionNames)
    class Generated(expectedFunctionNames: Set<String>) : TestState(expectedFunctionNames)
    fun functionDependenciesMatches(expectedFunctionNames: Set<String>) = this.expectedFunctionNames == expectedFunctionNames
  }

  suspend fun waitForTestGeneration(taskId: Int, expectedFunctionNames: Set<String>, timeout: Long = 5000) {
    val pending = TestState.Pending(expectedFunctionNames, CompletableDeferred())
    val currentTestState = testStates.compute(taskId) { _, state ->
      when (state) {
        is TestState.Generated -> state
        is TestState.Pending -> {
          if (!state.functionDependenciesMatches(expectedFunctionNames)) {
            state.signal.completeExceptionally(CancellationException("Function list changed")) // TODO Implement logger for this kind of things
          }
          state
        }
        null -> null
      }?.takeIf { it.functionDependenciesMatches(expectedFunctionNames) } ?: pending
    }

    if (currentTestState is TestState.Pending) {
      val result = withTimeoutOrNull(timeout) {
        currentTestState.signal.await()
      }
      result ?: throw CancellationException("Timeout exceeded for test generation $taskId")
    }
  }

  fun addTest(taskId: Int, dependencies: Map<String, List<String>>) {
    val newState = TestState.Generated(dependencies.keys)
    val oldState = testStates.put(taskId, newState)
    tests[taskId] = dependencies
    if (oldState is TestState.Pending) {
      oldState.signal.complete(Unit)
    }
  }

  fun getTest(taskId: Int): Map<String, List<String>>? = tests[taskId]

  override fun getState() = this

  override fun loadState(state: TestDependenciesManager) {
    tests.putAll(state.tests)
    for ((key, value) in tests) {
      testStates[key] = TestState.Generated(value.keys.toSet())
    }
  }

  fun isTestGenerated(taskId: Int, expectedFunctionNames: Set<String>): Boolean {
    val state = testStates[taskId]
    return state is TestState.Generated && state.functionDependenciesMatches(expectedFunctionNames)
  }

  companion object {
    fun getInstance(project: Project): TestDependenciesManager = project.service<TestDependenciesManager>()
  }
}