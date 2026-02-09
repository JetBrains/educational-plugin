package com.jetbrains.edu.coursecreator.framework.diff

import com.intellij.diff.merge.MergeModelBase
import com.intellij.diff.merge.MergeModelBase.State
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project

// BACKCOMPAT 2025.3. Merge it with `DiffConflictResolveStrategy.MyMergeModel`
abstract class MyMergeModelBase(
  project: Project,
  document: Document,
) : MergeModelBase<State>(project, document) {
  override fun onChangeUpdated(index: Int) {}
}