package com.jetbrains.edu.learning.actions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.courseFormat.eduAssistant.AiAssistantState
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.core.AssistantError
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.eduAssistant.ui.NextStepHintNotificationFrame
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NonNls
import java.awt.*
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.coroutines.EmptyCoroutineContext

class NextStepHintAction : ActionWithProgressIcon(), DumbAware {

  var actionTargetParent: JPanel? = null
  private var nextStepHintNotificationPanel: JComponent? = null
  private var highlighter: RangeHighlighter? = null
  private var getHintTask: GetHintTask? = null

  init {
    // TODO: should we customize it?
    templatePresentation.text = EduCoreBundle.message("action.Educational.Hint.text")
    setUpSpinnerPanel(PROCESS_MESSAGE)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (DumbService.isDumb(project)) {
      e.dataContext.showPopup(ActionUtil.getUnavailableMessage(EduCoreBundle.message("action.Educational.NextStepHint.description"), false))
      return
    }

    closeNextStepHintNotificationPanel()
    cancelTaskGettingHint()

    FileDocumentManager.getInstance().saveAllDocuments()
    val state = project.eduState ?: return
    val task = state.task

    getHintTask = GetHintTask(project, state, task)
    getHintTask?.let {
      ProgressManager.getInstance().run(it)
    }
  }

  private fun cancelTaskGettingHint() {
    if (getHintTask?.progressIndicator?.isRunning == true) {
      getHintTask?.progressIndicator?.cancel()
    }
  }

  private fun closeNextStepHintNotificationPanel() {
    nextStepHintNotificationPanel?.let {
      actionTargetParent?.remove(nextStepHintNotificationPanel)
      actionTargetParent?.revalidate()
      actionTargetParent?.repaint()
    }
    highlighter?.dispose()
  }

  @Suppress("DialogTitleCapitalization")
  private fun showNextStepHint(state: EduState, codeHint: String) =
    object : AnAction(EduCoreBundle.message("action.Educational.NextStepHint.show.code.text")) {
    override fun actionPerformed(p0: AnActionEvent) {
      highlighter?.dispose()
      val taskFile = state.taskFile
      val virtualFile = taskFile.getVirtualFile(state.project) ?: error("VirtualFile for ${taskFile.name} not found")
      val solutionContent = DiffContentFactory.getInstance().create(VfsUtil.loadText(virtualFile), virtualFile.fileType)
      val solutionAfterChangesContent = DiffContentFactory.getInstance().create(codeHint, virtualFile.fileType)
      val request = SimpleDiffRequest(
        EduCoreBundle.message("action.Educational.NextStepHint.description"),
        solutionContent,
        solutionAfterChangesContent,
        EduCoreBundle.message("action.Educational.NextStepHint.current.solution"),
        EduCoreBundle.message("action.Educational.NextStepHint.solution.after.changes")
      )
      val diffRequestChain = SimpleDiffRequestChain(request)
      diffRequestChain.putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, listOf(virtualFile.path))
      diffRequestChain.putUserData(NEXT_STEP_HINT_DIFF_FLAG, true)
      DiffManager.getInstance().showDiff(state.project, diffRequestChain, DiffDialogHints.FRAME)
      createDiffCloseListener(diffRequestChain, state)
    }
  }

  private fun createDiffCloseListener(diffRequestChain: SimpleDiffRequestChain, state: EduState) {
    val connection: MessageBusConnection = state.project.messageBus.connect()
    val myEditorChangeListener = object : FileEditorManagerListener {
      override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        if (file.name == EduCoreBundle.message("action.Educational.Diff.text")) {
          if (diffRequestChain.getUserData(NEXT_STEP_HINT_DIFF_FLAG) == true) {
            if (diffRequestChain.getUserData(IS_ACCEPTED_HINT) == null || diffRequestChain.getUserData(IS_ACCEPTED_HINT) == false) {
              val scope = CoroutineScope(EmptyCoroutineContext)
              val task = state.task
              val taskProcessor = TaskProcessor(task)
              val assistant = TaskBasedAssistant(taskProcessor)
              scope.launch {
                assistant.getTaskAnalysis(task)
              }
            }
            closeNextStepHintNotificationPanel()
            diffRequestChain.putUserData(IS_ACCEPTED_HINT, false)
            connection.disconnect()
          }
        }
        super.fileClosed(source, file)
      }
    }
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, myEditorChangeListener)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private inner class GetHintTask(private val project: Project, private val state: EduState, private val task: Task)
    : com.intellij.openapi.progress.Task.Backgroundable(project, EduCoreBundle.message("progress.title.getting.hint"), true) {

    var progressIndicator: ProgressIndicator? = null

    override fun run(indicator: ProgressIndicator) {
      processStarted()
      ApplicationManager.getApplication().executeOnPooledThread { EduActionUtils.showFakeProgress(indicator) }
      progressIndicator = indicator

      val taskProcessor = TaskProcessor(task)
      val assistant = TaskBasedAssistant(taskProcessor)
      runBlockingCancellable {
        task.aiAssistantState = AiAssistantState.HelpAsked
        val response = assistant.getHint(task, state)
        response.assistantError?.let {
          showHintWindow(it.errorMessage)
          return@runBlockingCancellable
        }
        response.textHint ?: run {
          showHintWindow(AssistantError.UnknownError.errorMessage)
          return@runBlockingCancellable
        }

        highlighter = response.codeHint?.let {
          highlightFirstCodeDiffPosition(project, state, it, indicator)
        }

        val action = response.codeHint?.let { showNextStepHint(state, it) }
        showHintWindow(response.textHint, action)
      }
      processFinished()
    }

    override fun onCancel() {
      super.onCancel()
      processFinished()
    }

    private fun showHintWindow(textToShow: String, action: AnAction? = null) {
      val nextStepHintNotification = NextStepHintNotificationFrame(textToShow, action, actionTargetParent) { closeNextStepHintNotificationPanel() }
      nextStepHintNotificationPanel = nextStepHintNotification.rootPane
      nextStepHintNotificationPanel?.let {  actionTargetParent?.add(it, BorderLayout.NORTH) }

      state.taskFile.getDocument(state.project)?.addDocumentListener(object : DocumentListener {
        override fun beforeDocumentChange(event: DocumentEvent) {
          super.beforeDocumentChange(event)
          closeNextStepHintNotificationPanel()
          cancelTaskGettingHint()
        }
      })
    }


    private fun highlightFirstCodeDiffPosition(project: Project, state: EduState, codeHint: String, indicator: ProgressIndicator): RangeHighlighter? {
      val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
      val virtualFile = state.taskFile.getVirtualFile(state.project) ?: return null
      val studentText = VfsUtil.loadText(virtualFile)
      val fragments = ComparisonManager.getInstance().compareLines(studentText, codeHint,
        ComparisonPolicy.DEFAULT, indicator)
      return fragments.firstOrNull()?.startLine1?.let { line ->
        val attributes = TextAttributes(
          null, HIGHLIGHTER_COLOR, null,
          EffectType.BOXED, Font.PLAIN
        )
        if (line < studentText.lines().size) {
          editor.markupModel.addLineHighlighter(line, 0, attributes)
        } else {
          null
        }
      }
    }
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.NextStepHint"
    private const val PROCESS_MESSAGE = "Getting hint in progress"
    private val HIGHLIGHTER_COLOR = JBColor(0xEFE5FF, 0x433358)

    val NEXT_STEP_HINT_DIFF_FLAG: Key<Boolean> = Key.create("nextStepHintDiffFlag")
    val IS_ACCEPTED_HINT: Key<Boolean> = Key.create("isAcceptedHint")
  }
}
