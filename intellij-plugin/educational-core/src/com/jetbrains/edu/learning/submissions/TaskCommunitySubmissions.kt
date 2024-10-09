package com.jetbrains.edu.learning.submissions

/**
 * A list of shared submissions and a boolean value indicating if there are more submissions to load.
 */
data class TaskCommunitySubmissions(val submissions: MutableList<Submission>, var hasMore: Boolean = false)