package com.jetbrains.edu.learning.actions

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getContainingTask
import okio.withLock
import java.util.concurrent.locks.ReentrantLock

/**
 * Manages information about a task that is currently run by a user.
 * It could be run either with the Run button from the Task tool window or
 * directly with a run button of some run configuration.
 *
 * We pretend that not more than one task runs at once, although a user might manually run
 * two different run configurations corresponding to different tasks.
 * We ignore this case and store only the first task as the one currently running.
 */
@Service(Service.Level.PROJECT)
class RunTaskActionState(private val project: Project) : Disposable {
  /**
   * All access to the internal state must be done under this lock
   */
  private val lock: ReentrantLock = ReentrantLock()

  private var runningTask: Task? = null
  private var taskProcessHandler: ProcessHandler? = null

  init {
    listenToExecutionOfTaskRunConfigurations()
  }

  private fun listenToExecutionOfTaskRunConfigurations() {
    val connection = project.messageBus.connect()
    Disposer.register(this, connection)
    connection.subscribe(ExecutionManager.EXECUTION_TOPIC, RunningTasksListener())
  }

  /**
   * Sets a task that is currently run.
   * The task will be successfully set only if no other task is currently running.
   * Returns null on success or the currently running task.
   */
  fun setRunningTask(task: Task): Task? = lock.withLock {
    if (runningTask != null) {
      return runningTask
    }
    runningTask = task
    return null
  }

  /**
   * Sets the [ProcessHandler] for some task.
   * The [ProcessHandler] is set only if the specified task is currently running, otherwise, nothing is done.
   */
  fun setProcessHandlerForTask(task: Task, processHandler: ProcessHandler) {
    lock.withLock {
      if (task == runningTask) {
        taskProcessHandler = processHandler
        return@withLock
      }
    }

    logger<RunTaskActionState>().warn("Trying to set a process handler for task $task that is not running")
  }

  /**
   * Clears the information about the running task. If the specified task is not running, nothing is done.
   */
  fun clearRunningTask(task: Task) = lock.withLock {
    if (runningTask == task) {
      runningTask = null
      taskProcessHandler = null
    }
  }

  /**
   * Stops the running task, returning its [ProcessHandler].
   * If the specified task is not running, nothing is done.
   */
  fun clearRunningTaskAndGetProcessHandler(task: Task): ProcessHandler? = lock.withLock {
    if (runningTask == task) {
      runningTask = null
      val processHandler = taskProcessHandler
      taskProcessHandler = null

      processHandler
    }
    else {
      null
    }
  }

  override fun dispose() {}

  /**
   * Listens to executions of any process.
   * If it is a run configuration corresponding to some task, then we pretend that this
   * task is running.
   */
  private inner class RunningTasksListener: ExecutionListener {
    override fun processStartScheduled(executorId: String, env: ExecutionEnvironment) {
      if (executorId != DefaultRunExecutor.EXECUTOR_ID) return

      val task = env.task ?: runReadAction {
        env.findTask()
      } ?: return

      setRunningTask(task)
    }

    @RequiresReadLock
    private fun ExecutionEnvironment.findTask(): Task? {
      val configurationPath = runnerAndConfigurationSettings?.pathIfStoredInArbitraryFileInProject ?: return null
      val configurationVirtualFile = LocalFileSystem.getInstance().findFileByPath(configurationPath) ?: return null
      val task = configurationVirtualFile.getContainingTask(project) ?: return null
      this.task = task

      return task
    }

    override fun processStarting(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
      val task = env.task ?: return
      setProcessHandlerForTask(task, handler)
    }

    override fun processTerminated(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int) {
      val task = env.task ?: return
      clearRunningTask(task)
    }

    override fun processNotStarted(executorId: String, env: ExecutionEnvironment, cause: Throwable?) {
      val task = env.task ?: return
      logger<RunTaskAction>().warn("Run configuration for task ${task.name} failed to start", cause)
      clearRunningTask(task)
    }
  }

  companion object {
    fun getInstance(project: Project): RunTaskActionState = project.service()
  }
}

private val taskOfEnvironmentKey = Key<Task>("task of execution environment")

var ExecutionEnvironment.task: Task?
  get() = getUserData(taskOfEnvironmentKey)
  set(value) {
    putUserData(taskOfEnvironmentKey, value)
  }