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

suspend fun processValidationHints(
  taskDescription: String,
  textHint: String,
  codeHint: String,
  userCode: String,
  solutionSteps: String
) =
  AiPlatformAdapter.chat(
    systemPrompt = buildHintsValidationSystemPrompt(),
    userPrompt = buildHintsValidationUserPrompt(taskDescription, textHint, codeHint, userCode, solutionSteps),
    temp = 0.0,
    generationContextProfile = AUTO_VALIDATION
  )

private val validationHintsCriteria = """
    1. information: Does the hint contain additional information, such as an explanation, tip or compliment? (If the hint contains additional information beyond solving the task, answer with "Yes" and specify what additional information. If it does not, answer with "No".)

    2. levelOfDetail: Is the hint a bottom-out hint (BOH) or a high-level description (HLD)? (If the hint is a bottom-out hint, answer with "BOH". If the hint is a high-level description, answer with "HLD".)

    3. personalized: Does the hint correlate with and enhance the student’s current code or approach? (If the hint includes modified student code, improved or enhanced to solve the task, answer with "Yes". If the hint does not correlate with the student's code, answer with "No" and specify why.)

    4. intersection: Is the next step not implemented in the student's code? (If the hint asks to implement something that is not in the student's code, answer with "Yes". If the hint asks to implement something that the student has already implemented, answer with "No" and specify what has already been implemented.)
    
    5. appropriate: Is the hint the correct and relevant next step to solve the problem, based on the task description, the solution steps and given the current state of the student's programme? ? (If the hint is directly working towards the solution of the problem in the task description, and follows the solution steps, given the current state of the student's code and does not ignore errors, answer with "Yes". If the hint is inconsistent with the task's goal, misleads the student, suggests incorrect actions or ignores any errors, answer with "No" and provide an explanation for your decision. An example of an inappropriate hint - a student uses var in his code when the task requires to use val, and the hint does not indicate this error.)
    
    6. specific: Is the hint limited to only one next step? (If the hint is limited to only one next step, answer with "Yes". If the hint contains several steps or the whole solution of the task, answer with "No" and indicate how many steps it contains.)
    
    7. misleadingInformation: Does the hint contain misleading information? (Misleading information includes, but is not limited to, proposing a solution that leads in a different direction, suggesting the implementation of an irrelevant function, or providing information that doesn't align with the task's requirements, or deviating from the solution steps or ignoring errors. If the hint does not contain any misleading information and assists in solving the task correctly, answer with "No". If the hint contains misleading information that might obstruct the student's progress towards the correct solution, answer with "Yes" and specify the misleading information.)
    
    8. codeQuality: Is generated code valid? (If the generated code has no bugs, answer with "Yes", otherwise answer with "No".)
    
    9. kotlinStyle: Is generated code in Kotlin-like style? (If the generated code conforms to common Kotlin language practices and idioms, answer with "Yes", otherwise answer with "No".)
    
    10. length: What is the length of the hint? (Answer with the number of sentences and number of words from the text hint and the number of new/changed/deleted code lines from the code hint.)
    
    11. correlationWithSteps: Does the hint correlate with the steps to solve the task? (If the hint guides the student by following these steps, depending on what step the student is currently at, answer with "Yes". If the hint contains guidance that does not match the steps or guidance for a step the student has already completed or for a step the student has not yet reached, answer with "No".)
  """.trimIndent()

private fun buildHintsValidationUserPrompt(
  taskDescription: String,
  textHint: String,
  codeHint: String,
  userCode: String,
  solutionSteps: String
) = """
    Determine the correctness of the hints using the given criteria.
    
    This is a description of the task, representing the goal the student should achieve in their code: <$taskDescription>
    The hints should be designed to guide the student towards fulfilling this task, directly aligning with its objective.
    
    Steps for solving the task: <$solutionSteps>
    The hint should follow these steps, looking at the student's code to see what step the student is currently in.
    
    Current student code: 
      ```kotlin
        $userCode
      ```
    
    Text hint: <$textHint>
    
    Code hint: 
      ```kotlin
        $codeHint
      ```
  """.trimIndent()

private fun buildHintsValidationSystemPrompt() = """
    Text hint and code hint have been generated to guide the student to the next step in solving the task. Your goal is to determine the correctness of these hints using the given criteria as if you were a teacher.
    
    The criteria: <$validationHintsCriteria>
    
    Format the response as json with keys: information, levelOfDetail, personalized, intersection, appropriate, specific, misleadingInformation, codeQuality, kotlinStyle, length.
    
    Find below an example response for reference:
    {
      "information": "No",
      "levelOfDetail": "HLD",
      "personalized": "Yes",
      "intersection": "No",
      "appropriate": "No, the student uses var in his code when the task requires to use val, and the hint does not indicate this error",
      "specific": "No, 2 steps",
      "misleadingInformation": "Yes, tries to call a function that does not exist",
      "codeQuality": "Yes",
      "kotlinStyle": "No, can be inlined",
      "length": "the text hint consists of 1 sentence and 12 words, the code hint consists of 1 new, 3 changed, 0 deleted code lines",
      "correlationWithSteps": "Yes"
    }
  """.trimIndent()

suspend fun processValidationHintForItsType(textHint: String, codeHint: String) =
  AiPlatformAdapter.chat(
    userPrompt = buildFeedbackTypePrompt(textHint, codeHint),
    temp = 0.0,
    generationContextProfile = AUTO_VALIDATION
  )

private val feedbackTypeCriterion = """
    Feedback type: What type of feedback is the generated hint? 
    Possible options: (select all the appropriate options)
      a) Knowledge about task constraints:
        i) Hints on task requirements, e.g. a hint that proposes to use a function or a programming construct to solve the task (or don’t use if the task forbids it). (If the hint contains task requirements, add "KTC-TR" to the answer)
        ii) Hints on task-processing rules. These hints provide general information on how to approach the exercise:
          1) TPR General - does not consider the current work of a student. (If the hint contains task-processing rules and does not consider the current work of a student, add "KTC-TPR-TPRG" to the answer)
          2) TPR Context - considers the current work of a student. (If the hint contains task-processing rules and considers the current work of a student, add "KTC-TPR-TPRC" to the answer)
      b) Knowledge about concepts:
        i) Explanations on subject matter, e.g. links to the documentation. (If the hint contains explanations on subject matter, add "KC-EXP" to the answer)
        ii) Examples illustrating concepts, e.g. examples of how to use a concept or a function. (If the hint contains examples illustrating concepts, add "KC-EXA" to the answer)
        iii) Examples that refers to the course content, e.g. refers to task steps or task hints. (If the hint contains examples that refers to the course content, add "KC-ECC" to the answer)
      c) Knowledge about mistakes:
        i) Test failures. (If the hint explains something about test failures, add "KM-TF" to the answer)
        ii) Solution errors, e.g. incorrect algorithm. (If the hint explains something about solution errors, add "KM-SE" to the answer)
        iii) Style issues, e.g. if the hint indicates that style issues should be corrected. (If the hint explains something about style issues, add "KM-SI" to the answer)
        iv) Performance issues. (If the hint explains something about performance issues, add "KM-PI" to the answer)
        v) Language issues, e.g. incorrect language. (If the hint explains something about language issues, add "KM-LI" to the answer)
      d) Knowledge about how to proceed:
        i) Bug-related hints for error correction. Mark a hint with this category if the feedback clearly focuses on what the student should do to correct a mistake:
          1) A hint that may be a in the form of a suggestion, a question, or an example. (If the hint contains a bug-related hint that is in the form of a suggestion, question, or example, add "KH-EC-ECH" to the answer)
          2) A solution that directly shows what needs to be done to correct an error. (If the hint contains a bug-related solution that directly shows what needs to be done to correct an error, add "KH-EC-ECS" to the answer)
          3) Both a hint and a solution. (If the hint contains both a bug-related hint and a bug-related solution, add "KH-EC-ECB" to the answer)
        ii) Task-processing steps. Information about the next step a student has to take to come closer to a solution:
          1) A hint that may be a in the form of a suggestion, a question, or an example. (If the hint contains a hint in the form of a suggestion, question, or example, and that hint includes information about the next step a student has to take to come closer to a solution, add "KH-TPS-TPSH" to the answer)
          2) A solution that directly shows what needs to be done to execute the next step. (If the hint contains a solution that directly shows what needs to be done to execute the next step, add "KH-TPS-TPSS" to the answer)
          3) Both a hint and a solution. (If the hint contains both a hint and a solution that shows what needs to be done to execute the next step, add "KH-TPS-TPSB" to the answer)
""".trimIndent()

private fun buildFeedbackTypePrompt(textHint: String, codeHint: String) = """
    Text hint and code hint have been generated to guide the student to the next step in solving the task. 
    Your goal is to feedback the type of the hint by choosing between the given options: <$feedbackTypeCriterion>
    
    Text hint: <$textHint>
    
    Code hint: 
      ```kotlin
        $codeHint
      ```
    
    Find below an example response for reference: "KTC-TPR-TPRC, KC-EXA, KH-TPS-TPSB"
  """.trimIndent()
