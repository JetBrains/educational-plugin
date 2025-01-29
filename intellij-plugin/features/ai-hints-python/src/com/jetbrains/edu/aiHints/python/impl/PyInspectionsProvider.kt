package com.jetbrains.edu.aiHints.python.impl

import com.jetbrains.edu.aiHints.core.api.InspectionsProvider

object PyInspectionsProvider : InspectionsProvider {
  /**
   * @see <a href=https://www.jetbrains.com/help/inspectopedia/Python.html>Python Inspections</a>
   * @see <a href=https://github.com/JetBrains-Research/code-quality-ij-server/blob/master/ij-core/src/main/kotlin/org/jetbrains/research/ij/headless/server/inspector/configs/python/PythonIJCodeInspectorConfig.kt#L7>Ignored inspections</a>
   */
  override val inspectionIds: Set<String>
    get() = setOf(
      "PyArgumentList",  // Quick-Fix is available in case of unexpected argument
      "PyChainedComparisons",
      "PyComparisonWithNone",
      "PyDictCreation",
      "PyExceptionInherit",
      "PyListCreation",
      "PyMethodParameters",
      "PyNoneFunctionAssignment",
      "PyRedundantParentheses",
      "PySimplifyBooleanCheck",
      "PyTrailingSemicolon",
    )
}