package com.jetbrains.edu.commandLine

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parsers.CommandLineParser
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
abstract class EduCommandTestBase<T : EduCommand>(
  private val commandData: EduCommandTestData<T>
) {

  protected abstract fun command(): T

  @Test
  fun `parse command arguments`() {
    val (args, expectedValues, errorMessage) = commandData
    val command = command()
    try {
      CommandLineParser.parseAndRun(command, args) {}
      Assert.assertNull("Argument parsing should fail", errorMessage)

      for ((property, expectedValue) in expectedValues) {
        Assert.assertEquals("`${property.name}` value wrong", expectedValue, property(command))
      }
    }
    catch (e: CliktError) {
      val formattedError = command.getFormattedHelp(e)
      Assert.assertEquals("Argument parsing fails with wrong message", errorMessage?.trimIndent(), formattedError)
    }
  }
}
