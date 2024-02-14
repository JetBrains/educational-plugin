package com.jetbrains.edu.learning.placeholder

import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder

/**
 * Checks that the placeholder is visible, and its dependency, if exists, is also visible.
 * The dependency for legacy reasons might also store information that it is invisible.
 */
val AnswerPlaceholder.isVisibleWithDependency: Boolean
  get() {
    val dependency = placeholderDependency

    return if (dependency == null) {
      isVisible
    }
    else {
      isVisible && dependency.isVisible
    }
  }