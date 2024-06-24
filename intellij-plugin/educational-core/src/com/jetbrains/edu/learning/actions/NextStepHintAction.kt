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
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
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
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.eduAssistant.AiAssistantState
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.hints.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.errors.NextStepHintError
import com.jetbrains.edu.learning.eduAssistant.log.Logger
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.edu.learning.eduAssistant.ui.NextStepHintNotificationFrame
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.awt.Font
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.JPanel

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

    FileDocumentManager.getInstance().saveAllDocuments()
    val state = project.eduState ?: return
    val task = state.task

    closeNextStepHintNotificationPanel()

    if (!GetHintTaskState.getInstance(project).lock()) {
      e.dataContext.showPopup(EduCoreBundle.message("action.Educational.NextStepHint.already.running"))
      return
    }

    getHintTask = GetHintTask(project, state, task).also {
      ProgressManager.getInstance().run(it)
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
  private fun showNextStepHint(state: EduState, taskFile: TaskFile, codeHint: String) =
    object : AnAction(EduCoreBundle.message("action.Educational.NextStepHint.show.code.text")) {
    override fun actionPerformed(p0: AnActionEvent) {
      highlighter?.dispose()
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
            when (diffRequestChain.getUserData(IS_ACCEPTED_HINT)) {
              null, false -> {
                if (actionTargetParent?.components?.contains(nextStepHintNotificationPanel) == true) {
                  rejectHint(state)
                }
              }
              true -> {
                val task = state.task
                Logger.eduAssistantLogger.info(
                  """Lesson id: ${task.lesson.id}    Task id: ${task.id}
                  |User response: accepted code hint
                  |
                """.trimMargin()
                )
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

  private fun rejectHint(state: EduState) {
    val task = state.task
    Logger.eduAssistantLogger.info(
      """Lesson id: ${task.lesson.id}    Task id: ${task.id}
        |User response: canceled code hint
        |
      """.trimMargin()
    )
    closeNextStepHintNotificationPanel()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private inner class GetHintTask(private val project: Project, private val state: EduState, private val task: Task)
    : com.intellij.openapi.progress.Task.Backgroundable(project, EduCoreBundle.message("progress.title.getting.hint"), true) {

    var progressIndicator: ProgressIndicator? = null

    override fun run(indicator: ProgressIndicator) {
      if (!GetHintTaskState.getInstance(project).isLocked) {
        showHintWindow(NextStepHintError.UnlockedError.errorMessage)
        return
      }

      processStarted()
      ApplicationManager.getApplication().executeOnPooledThread { EduActionUtils.showFakeProgress(indicator) }
      progressIndicator = indicator

      val taskProcessor = TaskProcessorImpl(task)
      runBlockingCancellable {
        task.aiAssistantState = AiAssistantState.HelpAsked
        val response = TaskBasedAssistant(taskProcessor).getHint()
        response.assistantException?.let {
          showHintWindow(it.message)
          return@runBlockingCancellable
        }
        response.textHint ?: run {
          showHintWindow(NextStepHintError.UnknownError.errorMessage)
          return@runBlockingCancellable
        }

        highlighter = response.codeHint?.let {
          highlightFirstCodeDiffPositionOrNull(taskProcessor.currentTaskFile ?: state.taskFile, it, indicator)
        }

        val action = response.codeHint?.let { showNextStepHint(state, taskProcessor.currentTaskFile ?: state.taskFile, it) }
        response.textHint?.let { showHintWindow(it, action) }
      }
    }

    override fun onFinished() {
      processFinished()
      GetHintTaskState.getInstance(project).unlock()
    }

    private fun showHintWindow(textToShow: String, action: AnAction? = null) {
      Logger.eduAssistantLogger.info(
        """Lesson id: ${task.lesson.id}    Task id: ${task.id}
        | User response: text shown
        | Text: $textToShow
      """.trimMargin()
      )
      task.status = CheckStatus.Unchecked
      TaskToolWindowView.getInstance(project).updateCheckPanel(task)
      val nextStepHintNotification = NextStepHintNotificationFrame(textToShow, action, actionTargetParent) { rejectHint(state) }
      nextStepHintNotificationPanel = nextStepHintNotification.rootPane
      nextStepHintNotificationPanel?.let { actionTargetParent?.add(it, BorderLayout.NORTH) }
    }


    /**
     * Highlights the first code difference position between the student's code in the task file and a given code hint.
     *
     * @return The range highlighter indicating the first code difference position, or null
     * if virtualFile or editor is null or
     * if the focus is on another file or
     * if no differences are found.
     */
    private fun highlightFirstCodeDiffPositionOrNull(taskFile: TaskFile, codeHint: String, indicator: ProgressIndicator): RangeHighlighter? {
      val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
      val virtualFile = taskFile.getVirtualFile(state.project) ?: return null
      val currentFile = FileDocumentManager.getInstance().getFile(editor.document)
      if (currentFile != virtualFile) {
        return null
      }
      val studentText = VfsUtil.loadText(virtualFile)
      val fragments = ComparisonManager.getInstance().compareLines(
        studentText, codeHint,
        ComparisonPolicy.DEFAULT, indicator
      )
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

  @Service(Service.Level.PROJECT)
  private class GetHintTaskState {
    private val isBusy = AtomicBoolean(false)

    val isLocked: Boolean
      get() = isBusy.get()

    fun lock(): Boolean {
      return isBusy.compareAndSet(false, true)
    }

    fun unlock() {
      isBusy.set(false)
    }

    companion object {
      fun getInstance(project: Project): GetHintTaskState = project.service()
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
