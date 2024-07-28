package com.jetbrains.edu.learning.services.dialog

/**
 * This interface should be refactored, see https://youtrack.jetbrains.com/issue/EDU-7090/Refactor-ServiceHostChanger
 * Consider not using it
 */
interface ServiceHostChanger {
  fun getResultUrl(): String?
}