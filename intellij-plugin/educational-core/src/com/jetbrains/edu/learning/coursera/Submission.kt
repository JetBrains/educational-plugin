package com.jetbrains.edu.learning.coursera

data class Submission(val assignmentKey: String, val submitterEmail: String,
                 val secret: String, val parts: Map<String, Part>)

data class Part(val output: String)

