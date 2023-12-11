package com.jetbrains.edu.coursecreator.framework.diff

import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonMergeUtil
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.fragments.MergeLineFragment
import com.intellij.diff.merge.MergeModelBase
import com.intellij.diff.tools.util.text.FineMergeLineFragmentImpl
import com.intellij.diff.tools.util.text.LineOffsetsUtil
import com.intellij.diff.tools.util.text.MergeInnerDifferences
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
      val documents = listOf(
        currentState[changedFile],
        resolvedSimpleConflictsState[changedFile],
        targetState[changedFile]
      ).map(::createTemporaryDocument)

      val baseDocument = documents[1]

      computeUnderProgress(project, EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.conflict.resolution.smart.indicator", changedFile)) {
        val allConflictsAreSolved = tryResolveConflictsForDocument(project, documents, changedFile, it)
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

  private fun createTemporaryDocument(content: CharSequence?): Document {
    return EditorFactory.getInstance().createDocument(content ?: "")
  }

  private fun tryResolveConflictsForDocument(
    project: Project,
    documents: List<Document>,
    changedFileName: String,
    indicator: ProgressIndicator,
  ): Boolean {
    require(documents.size == 3)

    val changes = calculateMergeLines(documents, indicator)
    val baseDocument = documents[1]

    val lineRanges = changes.map { change ->
      val startLine = change.getStartLine(ThreeSide.BASE)
      val endLine = change.getEndLine(ThreeSide.BASE)
      LineRange(startLine, endLine)
    }

    val model = MyMergeModel(project, baseDocument, lineRanges)

    var allConflictsResolved = true

    @Suppress("UnstableApiUsage")
    invokeAndWaitIfNeeded {
      model.executeMergeCommand(
        EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.conflict.resolution.smart.indicator", changedFileName),
        null,
        UndoConfirmationPolicy.DEFAULT,
        true,
        null,
      ) {
        for ((index, change) in changes.withIndex()) {
          val newContent = resolveChange(change, documents)
          if (newContent != null) {
            model.replaceChange(index, newContent)
          }
          else {
            allConflictsResolved = false
          }
        }
      }
    }

    return allConflictsResolved
  }

  private fun resolveChange(change: FineMergeLineFragmentImpl, documents: List<Document>): List<String>? {
    val changeType = change.conflictType

    if (isConflict(change.conflictType)) {
      if (!change.conflictType.canBeResolved()) {
        return null
      }
      val texts = ThreeSide.map { side ->
        val sourceDocument = side.select(documents)
        val startLine = change.getStartLine(side)
        val endLine = change.getEndLine(side)

        DiffUtil.getLinesContent(sourceDocument, startLine, endLine)
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

      val sourceDocument = sourceSide.select(documents)
      val startLine = change.getStartLine(sourceSide)
      val endLine = change.getEndLine(sourceSide)

      return DiffUtil.getLines(sourceDocument, startLine, endLine)
    }
  }

  private fun calculateMergeLines(
    documents: List<Document>,
    indicator: ProgressIndicator
  ): List<FineMergeLineFragmentImpl> {
    require(documents.size == 3)

    val contents = documents.map { it.charsSequence }
    val lineOffsets = contents.map { LineOffsetsUtil.create(it) }
    val fragments = ComparisonManager.getInstance().mergeLines(contents[0], contents[1], contents[2], comparisonPolicy, indicator)

    return fragments.map { change ->
      val changeType = MergeRangeUtil.getLineMergeType(change, contents, lineOffsets, comparisonPolicy)
      val innerDifference = getInnerDifference(change, changeType, documents, indicator)
      FineMergeLineFragmentImpl(change, changeType, innerDifference)
    }
  }

  private fun getInnerDifference(
    change: MergeLineFragment,
    changeType: MergeConflictType,
    documents: List<Document>,
    indicator: ProgressIndicator
  ): MergeInnerDifferences? {
    val chunks = ThreeSide.map { side ->
      if (!changeType.isChange(side)) return@map null

      val startLine = change.getStartLine(side)
      val endLine = change.getEndLine(side)
      if (startLine == endLine) return@map null

      val document = side.select(documents)
      DiffUtil.getLinesContent(document, startLine, endLine)
    }
    return DiffUtil.compareThreesideInner(chunks, comparisonPolicy, indicator)
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
