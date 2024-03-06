package com.jetbrains.edu.learning.eduAssistant.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.courseFormat.eduAssistant.AiAssistantState
import com.jetbrains.edu.learning.courseFormat.ext.getSolution
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.context.buildAuthorSolutionContext
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.FunctionSignatureResolver
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.createPsiFileForSolution
import com.jetbrains.edu.learning.eduAssistant.grazie.AiPlatformAdapter
import com.jetbrains.edu.learning.eduAssistant.grazie.AiPlatformException
import com.jetbrains.edu.learning.eduAssistant.inspection.applyInspections
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.messages.EduCoreBundle
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.jetbrains.edu.learning.eduAssistant.grazie.GenerationContextProfile.NEXT_STEP_TEXT_HINT
import com.jetbrains.edu.learning.eduAssistant.grazie.GenerationContextProfile.NEXT_STEP_CODE_HINT
import com.jetbrains.edu.learning.eduAssistant.grazie.GenerationContextProfile.SOLUTION_STEPS

class TaskBasedAssistant(private val taskProcessor: TaskProcessor) : Assistant {

  val logger = KotlinLogging.logger("EduAssistantLogger")
  var taskAnalysisPrompt: String? = null
  private var hintContext: HintContext? = null

  suspend fun getTaskAnalysis(task: Task): String? {
    if (taskProcessor.getFailureMessage() == EduCoreBundle.message("error.execution.failed")) return null

    task.authorSolutionContext ?: let {
      task.authorSolutionContext = task.buildAuthorSolutionContext()
    }

    hintContext = buildHintContext(task)
    taskAnalysisPrompt = buildTaskAnalysisPrompt(hintContext ?: error("Empty hintContext"))

    logger.info {
      """Task analysis prompt:
        |$taskAnalysisPrompt
      """.trimMargin()
    }

    return AiPlatformAdapter.chat(
      userPrompt = taskAnalysisPrompt ?: error("taskAnalysisPrompt was not initialized"),
      generationContextProfile = SOLUTION_STEPS
    ).deleteNoCodeSteps()
      .also {
      logger.info {
        """Task analysis response:
            |$it
          """.trimMargin()
      }
      task.generatedSolutionSteps = it
    }
  }

  private fun String.deleteNoCodeSteps(): String {
    val responseWithoutNonCodeSteps = this.replace(
      Regex("(?i)\\d+\\..*?no-code[\\s\\S]*?(?=\\n\\d+\\.|$)"),
      ""
    ).lines().filterNot { it.isBlank() }.joinToString(System.lineSeparator())
    var index = 1
    return Regex("""(?m)^\d+\.""").replace(responseWithoutNonCodeSteps) { "${index++}." }
  }

  override suspend fun getHint(task: Task, state: EduState, userCode: String?): AssistantResponse {
    logger.info {
      """Next step hint request
        |Lesson id: ${task.lesson.id}
        |Task id: ${task.id}
      """.trimMargin()
    }

    return try {
      if (taskProcessor.getFailureMessage() == EduCoreBundle.message("error.execution.failed")) return AssistantResponse(
         assistantError = AssistantError.NoCompiledCode
      )

      var codeStr = ""
      userCode?.let {
        codeStr = it
      } ?: ApplicationManager.getApplication().invokeAndWait {
        codeStr = taskProcessor.getSubmissionTextRepresentation() ?: ""
      }
      logger.info {
        """User code:
          |$codeStr
        """.trimMargin()
      }

      // The task files were not changed and the user asked about help again
      if (!taskProcessor.hasFilesChanged() && task.aiAssistantState == AiAssistantState.HelpAsked) getTaskAnalysis(task)
      val taskAnalysis = task.generatedSolutionSteps?.also { hintContext?.log() } ?: getTaskAnalysis(task) ?: return AssistantResponse(
        assistantError = AssistantError.NoCompiledCode
      )

      val nextStepTextHintPrompt = buildNextStepTextHintPrompt(taskAnalysis, codeStr)
      logger.info {
        """Text hint prompt:
          |$nextStepTextHintPrompt
        """.trimMargin()
      }
      val nextStepTextHint = AiPlatformAdapter.chat(userPrompt = nextStepTextHintPrompt, generationContextProfile = NEXT_STEP_TEXT_HINT)
      logger.info {
        """Text hint response:
          |$nextStepTextHint
        """.trimMargin()
      }

      val language = task.course.languageDisplayName.lowercase()
      val nextStepCodeHintPrompt = buildNextStepCodeHintPrompt(nextStepTextHint, codeStr, language)
      logger.info {
        """Code hint prompt:
             |$nextStepCodeHintPrompt
          """.trimMargin()
      }

      taskAnalysisPrompt ?: run {
        hintContext ?: run {
          hintContext = buildHintContext(task)
          taskAnalysisPrompt = buildTaskAnalysisPrompt(hintContext ?: error("Empty hintContext"))
        }
      }

      val prompts = mapOf(
        "taskAnalysisPrompt" to (taskAnalysisPrompt ?: error("taskAnalysisPrompt was not initialized!")),
        "nextStepTextHintPrompt" to nextStepTextHintPrompt,
        "nextStepCodeHintPrompt" to nextStepCodeHintPrompt
      )

      val codeHint = getNextStepCodeHint(nextStepCodeHintPrompt, language).also {
        logger.info {
          """Code hint response (before applying inspections):
            |$it
          """.trimMargin()
        }
      }
      val project = task.project ?: error("Project was not found")
      val languageId = task.course.languageById ?: error("Language was not found")
      val shortFunctionSignature = taskProcessor.getShortFunctionSignatureIfRecommended(codeHint, project, languageId)
      if (shortFunctionSignature != null) {
        val psiFileSolution = runReadAction { state.taskFile.getSolution().createPsiFileForSolution(project, languageId) }
        val nextStepCodeFromSolution = runReadAction {
          FunctionSignatureResolver.getFunctionBySignature(
            psiFileSolution, shortFunctionSignature, languageId
          )?.text
        }
        logger.info {
          """Code hint from solution:
             |$nextStepCodeFromSolution
          """.trimMargin()
        }
        AssistantResponse(
          nextStepTextHint,
          nextStepCodeFromSolution?.let { taskProcessor.applyCodeHint(it, state.taskFile) },
          prompts
        )
      }
      else {
        //TODO: investigate wrong cases
        val nextStepCodeHint = applyInspections(codeHint, project, languageId)
        logger.info {
          """Final code hint:
             |$nextStepCodeHint
          """.trimMargin()
        }
        AssistantResponse(nextStepTextHint, taskProcessor.applyCodeHint(nextStepCodeHint, state.taskFile), prompts)
      }
    }
    // TODO: Handle more exceptions with AiPlatformException
    catch (e: AiPlatformException) {
      AssistantResponse(assistantError = e.assistantError)
    }
    catch (e: Throwable) {
      AssistantResponse(assistantError = AssistantError.UnknownError)
    }
  }

  private fun buildTaskAnalysisPrompt(
    taskStr: String,
    functionsSetStrFromUserSolution: String?,
    functionsSetStrFromAuthorSolution: String?,
    hintsStr: String,
    theoryStr: String,
    availableForSolutionStrings: String?,
    language: String
  ) = """
    Generate an ordered list of steps to solve the described coding task with each step marked as either "code" or "no-code" in the title. For example, "no-code" steps might involve actions like "End ... function", "Test ... function", "Read the task description and understand the requirements". 
    Each step is a detailed explanation of one distinct concrete action or operation that is understandable for beginners in $language. Use built-in $language functions in your solution. Do not write any example code blocks, except names of functions that can be used in the solution.
    Be very detailed and precise, include at least 6 steps in the list, the more and clearer the better. 
    Here is information about the task, all blocks delimited with <>:
    
    Task: <$taskStr>
    
    Set of functions that might be implemented: <$functionsSetStrFromAuthorSolution>
    
    Existing functions within the user's implementation can be used for the solution, without needing to describe their implementations: <$functionsSetStrFromUserSolution>
    
    Hints: <$hintsStr>
    
    Theory: <$theoryStr>
    
    The solution can use only these strings: <${availableForSolutionStrings ?: "None"}>
  """.trimIndent()

  private fun buildNextStepTextHintPrompt(taskAnalysis: String, codeStr: String) = """
    Based on a sequence of steps to solve a coding problem, determine the next step that must be taken to complete the task.
    Here is the list of steps that solves the problem and the code, all delimited with <>:
    
    How to solve the task:
    <$taskAnalysis>
    
    Code:
    <$codeStr>
    
    ${buildTaskErrorInformation()}
    
    Given the present code and a sequence of steps to solve a coding problem, respond with a brief textual explanation in imperative form of what to do next in the problem-solving sequence.
    The response should not provide the complete solution, but instead focus on the next concise step that needs to be taken to make progress.
    Don't use the number of step in the response. Do not write any code, except names of functions that can be used in the solution.
  """.trimIndent()

  private fun buildTaskErrorInformation() = if (taskProcessor.taskHasErrors()) {
    """
      The present code does not pass test <${taskProcessor.getFailedTestName()}>, the error message is <${taskProcessor.getFailureMessage()}>.
      ${
      if (taskProcessor.getExpectedValue() != null && taskProcessor.getActualValue() != null) {
        """
          The expected test output was:
          ${taskProcessor.getExpectedValue()}
          But the actual test output was:
          ${taskProcessor.getActualValue()}
        """.trimIndent()
      }
      else ""
    }
      If the task appears to be completed, provide a step-by-step guide that explains what changes need to be made to the code to fix the failing test.
    """.trimIndent()
  }
  else {
    ""
  }

  private fun buildNextStepCodeHintPrompt(
    nextStepTextHint: String, codeStr: String, language: String
  ) = """
    Implement the next step to solve the coding task in the present code.
    Here is the description of the next step and the code, all delimited with <>:
    
    Next step: <$nextStepTextHint>
    
    Code:
    <$codeStr>
    
    Write the response in the following format:
    Response: <non-code text>
    Code: ```$language
    <code>
    ```
    
    If the implementation of the next step is to be made in a particular function or code block, the response should include the entire function or code block with the new changes incorporated into it. 
  """.trimIndent()

  private suspend fun getNextStepCodeHint(nextStepCodeHintPrompt: String, language: String, maxAttempts: Int = 3): String {
    return withContext(Dispatchers.Default) {
      var lastNextStepCodeHint: String? = null
      repeat(maxAttempts) { _ ->
        val nextStepCodeHint = AiPlatformAdapter.chat(
          userPrompt = nextStepCodeHintPrompt,
          temp = 0.1,
          generationContextProfile = NEXT_STEP_CODE_HINT
        )
        if (lastNextStepCodeHint == nextStepCodeHint) {
          error("Failed to generate the code hint.")
        }
        val result = getCodeFromResponse(nextStepCodeHint, language)
        if (!result.isNullOrBlank()) {
          return@withContext result
        }
        lastNextStepCodeHint = nextStepCodeHint
      }
      error("Failed to generate the code hint.")
    }
  }

  private fun getCodeFromResponse(response: String, language: String): String? {
    val pattern = """```$language(.*?)```""".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
    return pattern.find(response)?.groups?.get(1)?.value?.trim()
  }

  inner class HintContext(
    val taskStr: String,
    val functionsSetStrFromUserSolution: String?,
    val functionsSetStrFromAuthorSolution: String?,
    val hintsStr: String,
    val theoryStr: String,
    val availableForSolutionStrings: String?,
    val language: String
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
        logger.info {
          """Hint context - $name:
            |$it
          """.trimMargin()
        }
      }
    }
  }

  private fun buildTaskAnalysisPrompt(hintContext: HintContext): String = buildTaskAnalysisPrompt(
    taskStr = hintContext.taskStr,
    functionsSetStrFromUserSolution = hintContext.functionsSetStrFromUserSolution,
    functionsSetStrFromAuthorSolution = hintContext.functionsSetStrFromAuthorSolution,
    hintsStr = hintContext.hintsStr,
    theoryStr = hintContext.theoryStr,
    availableForSolutionStrings = hintContext.availableForSolutionStrings,
    language = hintContext.language
  )

  private fun buildHintContext(task: Task): HintContext {
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
      language = task.course.languageDisplayName.lowercase()
    )
  }
}
