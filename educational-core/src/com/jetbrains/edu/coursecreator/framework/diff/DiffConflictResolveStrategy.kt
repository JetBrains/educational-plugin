package com.jetbrains.edu.coursecreator.framework.diff

import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonMergeUtil
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.merge.MergeModelBase
import com.intellij.diff.tools.util.text.FineMergeLineFragmentImpl
import com.intellij.diff.tools.util.text.LineOffsets
import com.intellij.diff.tools.util.text.LineOffsetsUtil
import com.intellij.diff.util.*
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.LineTokenizer
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.messages.EduCoreBundle

class DiffConflictResolveStrategy(private val project: Project) : SimpleConflictResolveStrategy() {
  override fun resolveConflicts(
    currentState: Map<String, String>,
    baseState: Map<String, String>,
    targetState: Map<String, String>
  ): FLConflictResolveStrategy.StateWithResolvedChanges {
    // try to resolve simple conflicts
    val (changedFiles, resolvedSimpleConflictsState) = super.resolveConflicts(currentState, baseState, targetState)
    if (changedFiles.isEmpty()) {
      return FLConflictResolveStrategy.StateWithResolvedChanges(changedFiles, resolvedSimpleConflictsState)
    }
    val conflictFiles = mutableListOf<String>()
    val resolvedState = resolvedSimpleConflictsState.toMutableMap()
    for (changedFile in changedFiles) {
      // do not try to resolve if conflict isn't (modified, modified)
      if (currentState[changedFile] == null || baseState[changedFile] == null || targetState[changedFile] == null) {
        conflictFiles += changedFile
        continue
      }
      // must be not null
      val contents = listOf(
        currentState,
        resolvedSimpleConflictsState,
        targetState
      ).map {
        it[changedFile] ?: error("All contents should not be null")
      }

      // create a temporary document with base content for a merge model
      val baseDocument = EditorFactory.getInstance().createDocument(resolvedSimpleConflictsState[changedFile] ?: "")

      computeUnderProgress(project, EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.conflict.resolution.smart.indicator", changedFile)) {
        val allConflictsAreSolved = tryResolveConflictsForDocument(project, baseDocument, contents, changedFile, it)
        // do not change an original base text if the file contains unresolvable conflicts
        if (!allConflictsAreSolved) {
          conflictFiles += changedFile
        }
        else {
          resolvedState[changedFile] = baseDocument.text
        }
      }
    }
    return FLConflictResolveStrategy.StateWithResolvedChanges(conflictFiles, resolvedState)
  }

  private fun tryResolveConflictsForDocument(
    project: Project,
    baseDocument: Document,
    contents: List<String>,
    changedFileName: String,
    indicator: ProgressIndicator,
  ): Boolean {
    require(contents.size == 3)

    val lineOffsets = contents.map { LineOffsetsUtil.create(it) }
    val changes = calculateMergeLines(contents, lineOffsets, indicator)

    val lineRanges = changes.map { change ->
      val startLine = change.getStartLine(ThreeSide.BASE)
      val endLine = change.getEndLine(ThreeSide.BASE)
      LineRange(startLine, endLine)
    }

    val model = MyMergeModel(project, baseDocument, lineRanges)

    var allConflictsResolved = true

    @Suppress("UnstableApiUsage")
    invokeAndWaitIfNeeded {
      // the line below registers undo for task with given command id and launches a task with bulk update in write action
      model.executeMergeCommand(
        EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.conflict.resolution.smart.indicator", changedFileName),
        null,
        UndoConfirmationPolicy.DEFAULT,
        true,
        null,
      ) {
        for ((index, change) in changes.withIndex()) {
          val newContent = resolveChange(change, contents, lineOffsets)
          if (newContent != null) {
            model.replaceChange(index, newContent)
          }
          else {
            // if conflict could not be resolved automatically then break
            // there is no need to resolve any more conflicts
            // because version without any changes will be used anyway
            allConflictsResolved = false
            break
          }
        }
      }
    }

    return allConflictsResolved
  }

  private fun resolveChange(
    change: FineMergeLineFragmentImpl,
    contents: List<CharSequence>,
    lineOffsets: List<LineOffsets>
  ): List<String>? {
    val changeType = change.conflictType

    if (isConflict(change.conflictType)) {
      if (!change.conflictType.canBeResolved()) {
        return null
      }
      val texts = ThreeSide.map { side ->
        val content = side.select(contents)
        val sourceLineOffsets = side.select(lineOffsets)

        val startLine = change.getStartLine(side)
        val endLine = change.getEndLine(side)

        DiffRangeUtil.getLinesContent(content, sourceLineOffsets, startLine, endLine)
      }

      val newContent = ComparisonMergeUtil.tryResolveConflict(texts[0], texts[1], texts[2])
      if (newContent == null) {
        LOG.warn("Cannot resolve conflicting change: \n'${texts[0]}'\n'${texts[1]}'\n'${texts[2]}'")
        return null
      }

      val newContentLines = LineTokenizer.tokenize(newContent, false)
      return newContentLines.toList()
    }
    else {
      val sourceSide = if (changeType.isChange(Side.LEFT)) ThreeSide.LEFT else ThreeSide.RIGHT

      val sourceContent = sourceSide.select(contents)
      val sourceLineOffsets = sourceSide.select(lineOffsets)

      val startLine = change.getStartLine(sourceSide)
      val endLine = change.getEndLine(sourceSide)

      return DiffRangeUtil.getLines(sourceContent, sourceLineOffsets, startLine, endLine)
    }
  }

  private fun calculateMergeLines(
    contents: List<CharSequence>,
    lineOffsets: List<LineOffsets>,
    indicator: ProgressIndicator
  ): List<FineMergeLineFragmentImpl> {
    require(contents.size == 3)
    val fragments = ComparisonManager.getInstance().mergeLines(contents[0], contents[1], contents[2], comparisonPolicy, indicator)

    return fragments.map { change ->
      val changeType = MergeRangeUtil.getLineMergeType(change, contents, lineOffsets, comparisonPolicy)
      // we don't need inner differences because they are used only for highlighting
      FineMergeLineFragmentImpl(change, changeType, null)
    }
  }

  private fun isConflict(changeType: MergeConflictType): Boolean {
    return changeType.type == MergeConflictType.Type.CONFLICT
  }

  private class MyMergeModel(
    project: Project,
    document: Document,
    private val initialRanges: List<LineRange>,
  ) : MergeModelBase<MergeModelBase.State>(project, document) {
    init {
      setChanges(initialRanges)
    }

    override fun reinstallHighlighters(index: Int) {}

    override fun storeChangeState(index: Int): State {
      return State(index, initialRanges[index].start, initialRanges[index].end)
    }
  }

  companion object {
    private val comparisonPolicy = ComparisonPolicy.DEFAULT

    private val LOG = logger<DiffConflictResolveStrategy>()
  }
}
