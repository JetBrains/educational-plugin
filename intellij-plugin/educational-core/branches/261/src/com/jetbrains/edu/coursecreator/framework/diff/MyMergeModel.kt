package com.jetbrains.edu.coursecreator.framework.diff

import com.intellij.diff.merge.MergeModelBase
import com.intellij.diff.merge.MergeModelBase.State
import com.intellij.diff.util.LineRange
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project

// BACKCOMPAT 2025.3. Store it as a private class inside of `com.jetbrains.edu.coursecreator.framework.diff.DiffConflictResolveStrategy`
class MyMergeModel(
  project: Project,
  document: Document,
  private val initialRanges: List<LineRange>,
) : MergeModelBase<State>(project, document) {
  init {
    setChanges(initialRanges)
  }

  override fun onChangeUpdated(index: Int) {}

  override fun storeChangeState(index: Int): State {
    return State(index, initialRanges[index].start, initialRanges[index].end)
  }
}