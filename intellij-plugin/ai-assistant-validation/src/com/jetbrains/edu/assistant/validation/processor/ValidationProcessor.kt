package com.jetbrains.edu.assistant.validation.processor

import com.jetbrains.edu.learning.eduAssistant.grazie.AiPlatformAdapter
import com.jetbrains.edu.learning.eduAssistant.grazie.GenerationContextProfile.AUTO_VALIDATION

suspend fun processValidationSteps(taskDescription: String, authorSolution: String, steps: String) =
  AiPlatformAdapter.chat(
    systemPrompt = buildStepsValidationSystemPrompt(),
    userPrompt = buildStepsValidationUserPrompt(taskDescription, authorSolution, steps),
    temp = 0.0,
    generationContextProfile = AUTO_VALIDATION
  )

private val validationCriteria = """
    1. amount: The number of steps for the task. (Answer with the number of steps)

    2. specifics: Are the steps related to the specific actions? (A specific action in the context of a task resolution step can be defined as a clearly delineated instruction to perform a particular command, operation, or method in the code. If all steps relate to specific actions, such as creating a variable, calling a function or using a coding construction such as a loop or if statement, answer with "Yes". If there are broader and more abstract step(s) like "understand the requirements" or "analyze the provided examples" or "test the function" that don't specify a concrete action to be performed in the code, answer with "No" and specify which step(s) are not specific)

    3. independence: Are the steps independent? (If each step does not refer to previous steps, answer with "Yes". If there are step(s) that refer to other steps e.g. "repeat steps 3-5" or "... in step 2", answer with "No" and specify which step(s) are not independent)

    4. codingSpecific: Are the steps about coding tasks? (If each step is related to coding tasks such as adding a variable, changing the initial value etc, then answer with "Coding". If there are non-coding step(s) related to understanding the project, requirements gathering, or other tasks that don't involve direct interaction with the code like "read and understand the task" or "run the program", answer with "Not coding" and specify which step(s) have type "Not coding")

    5. direction: Do the steps solve the problem algorithmically? (If following all the steps results in a correct algorithm that accurately solves the task as per the task description, answer with "Yes"; if no, answer with "No" and specify why not and how it can be improved. You can compare the author's solution and the step solution, but it could be different and still be correct.)

    6. misleadingInformation: Does the steps contain misleading information? (If there are no such steps, answer "No". If there are step(s) that, for example, involves the use of non-existent functions, or the use of incorrect constants compared to the author's solution, or contains unnecessary additional steps that could be confusing, answer "Yes" and specify which step(s) and which misleading information)

    7. granularity: Is every step limited to one step? (If each step is an explanation of one distinct concrete action or operation, answer with "Yes". If there are step(s) that involve multiple actions, such as declaring multiple variables or operations, or declaring a variable and calling a function, which means they can be divided into several separate steps, answer with "No" and specify which step(s) and how they can be divided)

    8. kotlinStyle: Are the steps idiomatically correct in Kotlin language? (This means step explanations make use of built-in Kotlin functions and approaches appropriate to the language. Compare with the author's solution to see what Kotlin style functions are used. If all steps conform to Kotlin idiomatic style, answer with "Yes". If there are step(s) that are not, answer with "No" and specify which step(s) are not)
  """.trimIndent()

private fun buildStepsValidationUserPrompt(
  taskDescription: String,
  authorSolution: String,
  steps: String
) = """
    Determine the correctness of the list of steps using the given criteria.
    
    Task Description to be solved by following the list of steps: <$taskDescription>
    
    The list of steps for solving the task: <$steps>
    A list of steps is a numbered sequence of actions meant to be followed in order to solve the task. In this list, some steps might have substeps. Substeps are provided in the form of bullet points underneath their corresponding main step. When considering a step that has substeps, treat all of them together as the whole of that step. 
    
    Author's solution to the task: 
      ```kotlin
        $authorSolution
      ```
  """.trimIndent()

private fun buildStepsValidationSystemPrompt() = """
    The list of steps for solving the described coding task was generated. Your goal is to determine the correctness of this list of steps using the given criteria as if you were a teacher. 
    
    The criteria: <$validationCriteria>
    
    Format the response as json with keys: amount, specifics, independence, codingSpecific, direction, misleadingInformation, granularity, kotlinStyle.
    
    Find below an example response for reference:
    {
      "amount": "6",
      "specifics": "No, step 1 and step 6 are not specific",
      "independence": "Yes",
      "codingSpecific": "Not coding, step 1 and step 6 are not about coding tasks",
      "direction": "No, the steps do not solve the problem algorithmically. The steps do not mention the ... functions which are used in the author's solution. Also, the steps do not explain how ...",
      "misleadingInformation": "Yes. The step 3 mention the ... function which does not exist in the author's solution. The steps 2, 4 contain wrong and misleading string constants.",
      "granularity": "Yes",
      "kotlinStyle": "No, it does not recommend to use the "error" function"
    }
  """.trimIndent()
