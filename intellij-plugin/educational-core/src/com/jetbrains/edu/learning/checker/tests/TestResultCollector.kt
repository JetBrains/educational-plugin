package com.jetbrains.edu.learning.checker.tests

import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.tests.TestResultCollector.CollectorState.*
import java.util.concurrent.atomic.AtomicReference

/**
 * Collects test events (probably from different sources) and transforms them into unified data
 *
 * An instance of the class can be used only once because it holds the state of launched tests.
 *
 * @see SMTestResultCollector
 * @see CheckUtils.executeRunConfigurations
 */
abstract class TestResultCollector {

  private val state: AtomicReference<CollectorState> = AtomicReference(NOT_STARTED)

  /**
   * Subscribes and collects events about finished tests.
   * It's supposed the method can be invoked only once
   */
  fun startCollecting(connection: MessageBusConnection) {
    changeState(NOT_STARTED, IN_PROGRESS)
    Disposer.register(connection) {
      changeState(IN_PROGRESS, FINISHED)
    }

    doStartCollecting(connection)
  }

  /**
   * Transforms data collected by [startCollecting] into a list of [TestResultGroup].
   *
   * It's supposed the method can be invoked only after [startCollecting] and
   * only after receiving all tests events
   */
  fun collectTestResults(): List<TestResultGroup> {
    val currentState = state.get()
    if (currentState != FINISHED) {
      error("Unexpected collector state: expected: $FINISHED, actual: $currentState")
    }

    return doCollectTestResults()
  }

  /**
   * Subscribes on necessary topics using given [connection] to receive test events.
   */
  protected abstract fun doStartCollecting(connection: MessageBusConnection)

  /**
   * Transforms data collected in [doStartCollecting] into a list of [TestResultGroup]
   */
  protected abstract fun doCollectTestResults(): List<TestResultGroup>

  private fun changeState(expectedState: CollectorState, newState: CollectorState) {
    if (!state.compareAndSet(expectedState, newState)) {
      error("Unexpected collector state: expected: $expectedState, actual: ${state.get()}")
    }
  }

  private enum class CollectorState {
    NOT_STARTED,
    IN_PROGRESS,
    FINISHED
  }
}
