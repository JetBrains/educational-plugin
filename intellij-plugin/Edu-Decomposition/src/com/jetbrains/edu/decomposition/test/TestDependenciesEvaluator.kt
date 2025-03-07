package com.jetbrains.edu.decomposition.test

object TestDependenciesEvaluator {
  // TODO("""Implement evaluate function, suggested idea,
  //  verify if for every node of the generated graph its set of reachable
  //  nodes is contained in the student set of reachable nodes""")
  fun evaluate(expected: Map<String, List<String>>, current: Map<String, List<String>>): Boolean = suggested(expected, current)

  private fun default() = true

  private fun suggested(expected: Map<String, List<String>>, current: Map<String, List<String>>): Boolean =
    expected.entries.all { (key, values) ->
      current.containsKey(key) && (current[key]?.containsAll(values) ?: false)
    }
}