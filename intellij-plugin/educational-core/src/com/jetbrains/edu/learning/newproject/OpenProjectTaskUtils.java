package com.jetbrains.edu.learning.newproject;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.impl.OpenProjectTaskBuilder;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * DO NOT CONVERT IT INTO KOTLIN!
 * <p>
 * @see com.jetbrains.edu.learning.newproject.OpenProjectTaskUtilsKt#openProjectTask
 */
final class OpenProjectTaskUtils {
  static OpenProjectTask createOpenProjectTask(Function1<? super OpenProjectTaskBuilder, Unit> buildAction) {
    OpenProjectTaskBuilder builder = new OpenProjectTaskBuilder();
    return builder.build(buildAction);
  }
}
