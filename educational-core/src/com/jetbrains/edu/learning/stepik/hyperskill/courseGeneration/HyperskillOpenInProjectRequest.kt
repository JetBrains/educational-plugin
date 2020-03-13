package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

sealed class HyperskillOpenInProjectRequest(val projectId: Int)
class HyperskillOpenStepRequest(projectId: Int, val stepId: Int) : HyperskillOpenInProjectRequest(projectId)
class HyperskillOpenStageRequest(projectId: Int, val stageId: Int?) : HyperskillOpenInProjectRequest(projectId)