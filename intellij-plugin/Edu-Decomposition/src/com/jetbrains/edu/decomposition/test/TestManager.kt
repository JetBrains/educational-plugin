package com.jetbrains.edu.decomposition.test

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

/**
 * Manages the generated test for the specific task and keeps it in a State component avoiding generating more than once
 */
@Service(Service.Level.PROJECT)
@State(name = "TestManager", storages = [Storage("storage.xml")])
class TestManager : PersistentStateComponent<TestManager> {
  private val tests = mutableMapOf<Int, Map<String, List<String>>>()

  fun addTest(taskId: Int, dependencies: Map<String, List<String>>) {
    tests[taskId] = dependencies
  }

  fun getTest(taskId: Int) = tests[taskId]

  override fun getState() = this

  override fun loadState(state: TestManager) {
    tests.putAll(state.tests)
  }

  fun isTestGenerated(taskId: Int, functionNames: List<String>): Boolean {
    return tests[taskId]?.values?.toSet()?.equals(functionNames.toSet()) ?: false
  }

  companion object {
    fun getInstance(project: Project): TestManager = project.service<TestManager>()
  }
}