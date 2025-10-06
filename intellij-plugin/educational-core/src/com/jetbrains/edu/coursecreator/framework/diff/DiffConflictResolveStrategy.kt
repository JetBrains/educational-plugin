package com.jetbrains.edu.coursecreator.framework.diff

import com.intellij.diff.comparison.ComparisonMergeUtil
import com.intellij.diff.merge.MergeModelBase
import com.intellij.diff.tools.util.base.HighlightPolicy
import com.intellij.diff.tools.util.base.IgnorePolicy
import com.intellij.diff.tools.util.base.TextDiffSettingsHolder
import com.intellij.diff.tools.util.text.FineMergeLineFragment
import com.intellij.diff.tools.util.text.LineOffsetsUtil
import com.intellij.diff.tools.util.text.SimpleThreesideTextDiffProvider
import com.intellij.diff.util.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.LineTokenizer
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.messages.EduCoreBundle

class DiffConflictResolveStrategy(private val project: Project) : FLConflictResolveStrategyBase(), Disposable {
  private val diffSettings = TextDiffSettingsHolder.TextDiffSettings().apply {
    highlightPolicy = HighlightPolicy.DO_NOT_HIGHLIGHT
    ignorePolicy = IgnorePolicy.DEFAULT
  }

  private val textDiffProvider = SimpleThreesideTextDiffProvider(
    diffSettings,
    DiffUserDataKeys.ThreeSideDiffColors.MERGE_CONFLICT,
    {},
    this
  )

  override fun dispose() {}

  override fun resolveConflicts(
    currentState: FLTaskState,
    baseState: FLTaskState,
    targetState: FLTaskState,
  ): FLConflictResolveStrategy.StateWithResolvedChanges {
    // try to resolve simple conflicts
    val (changedFiles, resolvedSimpleConflictsState) = resolveSimpleConflicts(currentState, baseState, targetState)
    if (changedFiles.isEmpty()) {
      return FLConflictResolveStrategy.StateWithResolvedChanges(changedFiles, resolvedSimpleConflictsState)
    }
    val conflictFiles = mutableListOf<String>()
    val resolvedState = resolvedSimpleConflictsState.toMutableMap()

    computeUnderProgress(project, EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.conflict.resolution.smart.indicator")) { indicator ->
      for (changedFile in changedFiles) {
        val leftContent = currentState[changedFile]
        val baseContent = baseState[changedFile]
        val rightContent = targetState[changedFile]

        // do not try to resolve if conflict isn't (modified, modified)
        if (leftContent == null || baseContent == null || rightContent == null) {
          conflictFiles += changedFile
          continue
        }

        // do not try to resolve conflicts with binary files
        if (leftContent is BinaryContents || baseContent is BinaryContents || rightContent is BinaryContents) {
          conflictFiles += changedFile
          continue
        }

        val resolvedBaseContent = resolvedSimpleConflictsState[changedFile] ?: error("Base state with resolved conflicts shouldn't be null")

        val contents = ThreeSideContentInfo(leftContent, resolvedBaseContent, rightContent)

        // create a temporary document with base content for a merge model
        val baseDocument = EditorFactory.getInstance().createDocument(contents.baseContent.textualRepresentation)

        val allConflictsAreSolved = tryResolveConflictsForDocument(project, baseDocument, contents, indicator)
        // do not change an original base text if the file contains unresolvable conflicts
        if (!allConflictsAreSolved) {
          conflictFiles += changedFile
        }
        else {
          resolvedState[changedFile] = InMemoryTextualContents(baseDocument.text)
        }
      }
    }
    return FLConflictResolveStrategy.StateWithResolvedChanges(conflictFiles, resolvedState)
  }

  private fun tryResolveConflictsForDocument(
    project: Project,
    baseDocument: Document,
    contentInfo: ThreeSideContentInfo,
    indicator: ProgressIndicator,
  ): Boolean {
    val changes = textDiffProvider.compare(
      contentInfo.leftContent.textualRepresentation,
      contentInfo.baseContent.textualRepresentation,
      contentInfo.rightContent.textualRepresentation,
      indicator
    )

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
        EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.conflict.resolution.smart.indicator"),
        null,
        UndoConfirmationPolicy.DEFAULT,
        true,
        null,
      ) {
        for ((index, change) in changes.withIndex()) {
          val newContent = resolveChange(change, contentInfo)
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
    change: FineMergeLineFragment,
    contentInfo: ThreeSideContentInfo,
  ): List<String>? {
    val changeType = change.conflictType

    if (isConflict(change.conflictType)) {
      if (!change.conflictType.canBeResolved()) {
        return null
      }
      val texts = ThreeSide.map { side ->
        val content = side.select(contentInfo.contents).textualRepresentation
        val offsets = LineOffsetsUtil.create(content)

        val startLine = change.getStartLine(side)
        val endLine = change.getEndLine(side)

        DiffRangeUtil.getLinesContent(content, offsets, startLine, endLine)
      }

      val content = ComparisonMergeUtil.tryResolveConflict(texts[0], texts[1], texts[2])
      if (content == null) {
        LOG.warn("Cannot resolve conflicting change: \n'${texts[0]}'\n'${texts[1]}'\n'${texts[2]}'")
        return null
      }

      val contentLines = LineTokenizer.tokenize(content, false)
      return contentLines.toList()
    }
    else {
      val side = if (changeType.isChange(Side.LEFT)) ThreeSide.LEFT else ThreeSide.RIGHT

      val content = side.select(contentInfo.contents).textualRepresentation
      val offsets = LineOffsetsUtil.create(content)

      val startLine = change.getStartLine(side)
      val endLine = change.getEndLine(side)

      return DiffRangeUtil.getLines(content, offsets, startLine, endLine)
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

  private class ThreeSideContentInfo(
    val leftContent: FileContents,
    val baseContent: FileContents,
    val rightContent: FileContents,
  ) {
    val contents: List<FileContents> = listOf(leftContent, baseContent, rightContent)
  }

  companion object {
    private val LOG = logger<DiffConflictResolveStrategy>()
  }
}
