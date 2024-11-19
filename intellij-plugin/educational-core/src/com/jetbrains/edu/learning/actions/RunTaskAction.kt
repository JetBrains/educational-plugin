package com.jetbrains.edu.learning.actions

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Key
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.awt.AnchoredPoint
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.checker.CheckUtils.getCustomRunConfigurationForRunner
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.edu.learning.ui.isDefault
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent

class RunTaskAction : ActionWithButtonCustomComponent(), DumbAware {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    val taskFile = project.selectedTaskFile ?: return
    val task = taskFile.task

    val runConfiguration = getCustomRunConfigurationForRunner(project, task) ?: return
    // store the configuration found during the update to use it later when the action is performed
    e.presentation.putClientProperty(RUN_CONFIGURATION, runConfiguration)
    e.presentation.putClientProperty(SHOW_TEXT_IN_TOOLBAR, true)
    e.presentation.putClientProperty(SHOW_AS_DEFAULT_BUTTON, task is TheoryTask)

    e.presentation.text = EduCoreBundle.message("action.run.button.text")
    e.presentation.description = EduCoreBundle.message("action.run.button.description", task.name)

    e.presentation.isEnabledAndVisible = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val taskFile = project.selectedTaskFile ?: return
    val task = taskFile.task

    runTask(project, task, e)
  }

  override fun createCustomComponent(presentation: Presentation, place: String): JButton {
    val component = super.createCustomComponent(presentation, place)
    setupButton(component, presentation)
    return component
  }

  override fun updateCustomComponent(component: JComponent, presentation: Presentation) {
    super.updateCustomComponent(component, presentation)
    val button = component as? JButton ?: return
    setupButton(button, presentation)
    button.invalidate()
    button.repaint()
  }

  private fun setupButton(button: JButton, presentation: Presentation) {
    button.isDefault = presentation.getClientProperty(SHOW_AS_DEFAULT_BUTTON) ?: false
  }

  private fun runTask(project: Project, task: Task, e: AnActionEvent?) {
    val runningTask = RunTaskActionState.getInstance(project).setRunningTask(task)
    if (runningTask != null) {
      if (e != null) {
        showPopupTaskIsAlreadyRunning(project, runningTask, task, e)
      }
      return
    }

    val runnerAndConfigurationSettings =
      e?.presentation?.getClientProperty(RUN_CONFIGURATION) ?:
      getCustomRunConfigurationForRunner(project, task)

    if (runnerAndConfigurationSettings == null) {
      logger<RunTaskAction>().warn("Failed to find custom run configuration for runner of ${task.name}")
      return
    }

    ApplicationManager.getApplication().executeOnPooledThread {
      checkSettingsAndRun(runnerAndConfigurationSettings, project, task)
    }
  }

  private fun checkSettingsAndRun(
    runnerAndConfigurationSettings: RunnerAndConfigurationSettings,
    project: Project,
    task: Task
  ) {
    try {
      runnerAndConfigurationSettings.checkSettings()
    }
    catch (e: RuntimeConfigurationException) {
      logger<RunTaskAction>().warn("Custom run configuration \"${runnerAndConfigurationSettings.name}\" has warnings in settings: ${e.messageHtml}")
    }
    catch (e: RuntimeConfigurationError) {
      RunTaskActionState.getInstance(project).clearRunningTask(task)
      logger<RunTaskAction>().warn("Custom run configuration \"${runnerAndConfigurationSettings.name}\" has an error in settings: ${e.messageHtml}")
      return
    }

    val runner = ProgramRunner.getRunner(DefaultRunExecutor.EXECUTOR_ID, runnerAndConfigurationSettings.configuration)
    if (runner == null) {
      logger<RunTaskAction>().error("Failed to find runner for custom run configuration: ${runnerAndConfigurationSettings.name}")
      RunTaskActionState.getInstance(project).clearRunningTask(task)
      return
    }
    val env = ExecutionEnvironmentBuilder.create(
      DefaultRunExecutor.getRunExecutorInstance(),
      runnerAndConfigurationSettings
    ).activeTarget().build()

    env.task = task

    runInEdt {
      try {
        runner.execute(env)
      }
      catch (e: ExecutionException) {
        logger<RunTaskAction>().warn("Failed to run custom run configuration: ${runnerAndConfigurationSettings.name}", e)
        RunTaskActionState.getInstance(project).clearRunningTask(task)
      }
    }
  }

  private fun showPopupTaskIsAlreadyRunning(
    project: Project,
    runningTask: Task,
    nextTask: Task,
    e: AnActionEvent
  ) {
    val alreadyRunningMessage = if (runningTask == nextTask) {
      EduCoreBundle.message("actions.run.task.rerun.this")
    }
    else {
      EduCoreBundle.message("actions.run.task.stop.that.run.this", runningTask.name)
    }

    val stopAndRunHandler = StopAndRun(project, runningTask, nextTask)

    val balloon = JBPopupFactory.getInstance()
      .createHtmlTextBalloonBuilder(
        alreadyRunningMessage,
        null,
        UIUtil.getToolTipActionBackground(),
        stopAndRunHandler
      )
      .setFadeoutTime(5000)
      .createBalloon()

    stopAndRunHandler.balloon = balloon

    val pressedButton = e.getData(CONTEXT_COMPONENT) ?: return
    val tooltipRelativePoint = AnchoredPoint(AnchoredPoint.Anchor.TOP, pressedButton)
    balloon.show(tooltipRelativePoint, Balloon.Position.above)
  }

  private inner class StopAndRun(
    private val project: Project,
    private val runningTask: Task,
    private val nextTask: Task
  ) : HyperlinkAdapter() {
    var balloon: Balloon? = null

    override fun hyperlinkActivated(e: HyperlinkEvent) {
      balloon?.hideImmediately()

      val processHandler = RunTaskActionState.getInstance(project).clearRunningTaskAndGetProcessHandler(runningTask)
      processHandler?.destroyProcess()

      runTask(project, nextTask, null)
    }
  }

  companion object {
    const val ACTION_ID = "Educational.Run"
    const val RUN_CONFIGURATION_FILE_NAME = "runner.run.xml"

    private val RUN_CONFIGURATION = Key<RunnerAndConfigurationSettings>("run configuration")
    private val SHOW_AS_DEFAULT_BUTTON = Key<Boolean>("show as default button")
  }
}