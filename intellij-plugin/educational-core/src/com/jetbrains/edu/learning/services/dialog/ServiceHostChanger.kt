package com.jetbrains.edu.learning.services.dialog

/**
 * This interface should be refactored, consider not using it.
 *
 * See [EDU-7090: Refactor ServiceHostChanger](https://youtrack.jetbrains.com/issue/EDU-7090/Refactor-ServiceHostChanger)
 */
interface ServiceHostChanger {
  fun getResultUrl(): String?
}