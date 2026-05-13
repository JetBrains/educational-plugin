package com.jetbrains.edu.coursecreator.framework.diff

import com.intellij.diff.merge.MergeModelBase
import com.intellij.diff.merge.MergeModelBase.State
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

// BACKCOMPAT 2025.3. Merge it with `DiffConflictResolveStrategy.MyMergeModel`
abstract class MyMergeModelBase(
  project: Project,
  document: Document,
) : MergeModelBase<State>(project, document) {
  override fun onChangeUpdated(index: Int) {}
}

// BACKCOMPAT 2025.3. Inline it
typealias FilesCollection = Collection<VirtualFile>