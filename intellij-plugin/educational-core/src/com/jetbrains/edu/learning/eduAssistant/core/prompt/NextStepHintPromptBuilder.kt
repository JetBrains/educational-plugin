package com.jetbrains.edu.learning.eduAssistant.core.prompt

import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant.Companion.logEduAssistantInfo
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor

class NextStepHintPromptBuilder : PromptBuilder {

  override fun buildTextHintPrompt(codeHint: String, codeStr: String) = """
    Based on the given code and the improved version of the code, provide a concise textual hint that directly guides to improve the given code.
    Here is the current code and the improved version of the code, all delimited with <>:
    
    The code:
    <$codeStr>
    
    The improved version of the code:
     <$codeHint>
    
    Respond with a brief textual instruction in imperative form of what modifications need to be made to the code to achieve the improvements exhibited in the improved code. 
    Do not write any code, except names of functions or string literals. 
    Avoid explaining why the modifications are needed.
  """.trimIndent()

  override fun buildTextHintPromptIfNoCodeHintIsGenerated(taskProcessor: TaskProcessor, codeStr: String) =
    buildNextStepTextHintPromptIfNoCodeHintIsGenerated(taskProcessor, codeStr, buildHintContext(taskProcessor))

  private fun buildNextStepTextHintPromptIfNoCodeHintIsGenerated(
    taskProcessor: TaskProcessor,
    codeStr: String,
    hintContext: HintContext
  ) = """
    Based on a coding problem, determine the next step that must be taken to complete the task.
    Here is the list of steps that solves the problem and the code, all delimited with <>:
    
    Task: <${hintContext.taskStr}>
    
    Set of functions that might be implemented: <${hintContext.functionsSetStrFromAuthorSolution}>
    
    Existing functions within the user's implementation can be used for the solution, without needing to describe their implementations: <${hintContext.functionsSetStrFromUserSolution}>
    
    Hints: <${hintContext.hintsStr}>
    
    Theory: <${hintContext.theoryStr}>
    
    The solution can use only these strings: <${hintContext.availableForSolutionStrings ?: "None"}>
    
    The code:
    <$codeStr>
    
    ${buildTaskErrorInformation(taskProcessor)}
    
    Respond with a brief textual instruction in imperative form of what to do next in the problem-solving sequence.
    The response should not provide the complete solution, but instead focus on the next concise step that needs to be taken to make progress.
    Don't use the number of step in the response. Do not write any code, except names of functions that can be used in the solution.
  """.trimIndent()

  override fun buildCodeHintPrompt(taskProcessor: TaskProcessor, codeStr: String, language: String) =
    buildNextStepCodeHintPrompt(taskProcessor, codeStr, language, buildHintContext(taskProcessor))

  private fun buildNextStepCodeHintPrompt(
    taskProcessor: TaskProcessor,
    codeStr: String,
    language: String,
    hintContext: HintContext
  ) = formatCodeResponsePrompt("""
    Generate a modified version of the provided student's code that incorporates the next step towards the solution for the described coding task. 
    The modified code should not be a complete solution but should represent the next logical step that the student needs to take to solve the task. 
    Try to maintain the original structure of the student's code and focus especially on addressing common errors, while guiding the student towards the correct implementation.
    Here is information about the task and the student's code, all blocks delimited with <>:
    
    Task: <${hintContext.taskStr}>
    
    Set of functions that might be implemented: <${hintContext.functionsSetStrFromAuthorSolution}>
    
    Existing functions within the user's implementation can be used for the solution, without needing to describe their implementations: <${hintContext.functionsSetStrFromUserSolution}>
    
    Hints: <${hintContext.hintsStr}>
    
    Theory: <${hintContext.theoryStr}>
    
    The solution can use only these strings: <${hintContext.availableForSolutionStrings ?: "None"}>

  """.trimIndent(), codeStr, language, taskProcessor)

  override fun buildCodeHintPromptFromTextHint(
    taskProcessor: TaskProcessor, textHint: String, codeStr: String, language: String
  ) = formatCodeResponsePrompt("""
    Implement the next step to solve the coding task in the present code.
    Here is the description of the next step and the code, all delimited with <>:
    
    Next step: <$textHint>
  """.trimIndent(), codeStr, language, taskProcessor)

  inner class HintContext(
    val taskStr: String,
    val functionsSetStrFromUserSolution: String?,
    val functionsSetStrFromAuthorSolution: String?,
    val hintsStr: String,
    val theoryStr: String,
    val availableForSolutionStrings: String?,
    val language: String,
    val taskProcessor: TaskProcessor
  ) {
    init {
      log()
    }

    fun log() {
      log("TASK TEXT", taskStr)
      log("FUNCTIONS SET FROM USER SOLUTION", functionsSetStrFromUserSolution)
      log("FUNCTIONS SET FROM AUTHOR SOLUTION", functionsSetStrFromAuthorSolution)
      log("TASK HINTS", hintsStr)
      log("THEORY SUMMARY", theoryStr)
      log("STRINGS SET", availableForSolutionStrings)
    }

    private fun log(name: String, value: String?) {
      value?.let {
        logEduAssistantInfo(
          taskProcessor,
          """Hint context - $name:
            |$it
          """.trimMargin()
        )
      }
    }
  }

  private fun buildHintContext(taskProcessor: TaskProcessor): HintContext {
    val task = taskProcessor.task
    val functionsSetStrFromUserSolution = taskProcessor.getFunctionsFromTask()
    val functionsSetStrFromAuthorSolution = task.authorSolutionContext?.functionSignatures?.filterNot {
      functionsSetStrFromUserSolution?.contains(
        it
      ) == true
    }
    val userStrings = taskProcessor.getStringsFromTask()
    val availableForSolutionStrings = task.authorSolutionContext?.functionsToStringMap?.values?.flatten()?.filterNot {
      userStrings.contains(
        it
      )
    }

    return HintContext(
      taskStr = taskProcessor.getTaskTextRepresentation(),
      functionsSetStrFromUserSolution = functionsSetStrFromUserSolution?.joinToString(separator = System.lineSeparator()),
      functionsSetStrFromAuthorSolution = functionsSetStrFromAuthorSolution?.joinToString(separator = System.lineSeparator()),
      hintsStr = taskProcessor.getHintsTextRepresentation().joinToString(separator = System.lineSeparator()),
      theoryStr = taskProcessor.getTheoryTextRepresentation(),
      availableForSolutionStrings = availableForSolutionStrings?.joinToString(separator = System.lineSeparator()),
      language = task.course.languageDisplayName.lowercase(),
      taskProcessor = taskProcessor
    )
  }
}
