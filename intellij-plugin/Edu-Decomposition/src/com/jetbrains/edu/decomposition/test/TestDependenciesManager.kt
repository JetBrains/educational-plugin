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

  private sealed class TestState {
    class Pending(val expectedFunctionNames: Set<String>, val signal: CompletableDeferred<Unit>) : TestState()
    class Generated(val expectedFunctionNames: Set<String>) : TestState()
  }

  suspend fun waitForTestGeneration(taskId: Int, functionNames: List<String>, timeout: Long = 5000) {
    val expectedFunctionNames = functionNames.toSet()
    val pending = TestState.Pending(expectedFunctionNames, CompletableDeferred())
    val currentTestState = testStates.compute(taskId) { _, state ->
      when (state) {
        is TestState.Generated ->
          state.takeIf { state.expectedFunctionNames == expectedFunctionNames } ?: pending
        is TestState.Pending -> {
          if (state.expectedFunctionNames != expectedFunctionNames) {
            state.signal.completeExceptionally(CancellationException("Function list changed")) // TODO Implement logger for this kind of things
          }
          state.takeIf { state.expectedFunctionNames == expectedFunctionNames } ?: pending
        }
        null -> pending
      }
    }

    if (currentTestState is TestState.Pending) {
      val result = withTimeoutOrNull(timeout) {
        currentTestState.signal.await()
      }
      result ?: throw CancellationException("Timeout exceeded for test generation $taskId")
    }
  }

  fun addTest(taskId: Int, dependencies: Map<String, List<String>>) {
    val expectedFunctionNames = dependencies.keys.toSet()
    val newState = TestState.Generated(expectedFunctionNames)
    val oldState = testStates.put(taskId, newState)
    if (oldState is TestState.Pending) {
      oldState.signal.complete(Unit)
    }
    tests[taskId] = dependencies
  }

  fun getTest(taskId: Int): Map<String, List<String>>? = tests[taskId]

  override fun getState() = this

  override fun loadState(state: TestDependenciesManager) {
    tests.putAll(state.tests)
    for ((key, value) in tests) {
      testStates[key] = TestState.Generated(value.keys.toSet())
    }
  }

  fun isTestGenerated(taskId: Int, functionNames: List<String>): Boolean {
    val state = testStates[taskId]
    val expectedFunctionNames = functionNames.toSet()
    return state is TestState.Generated && state.expectedFunctionNames == expectedFunctionNames
  }

  companion object {
    fun getInstance(project: Project): TestDependenciesManager = project.service<TestDependenciesManager>()
  }
}