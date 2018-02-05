package com.jetbrains.edu.learning.ui.taskDescription;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Condition;

/**
 * author: liana
 * data: 7/29/14.
 */
public class EduCondition implements Condition, DumbAware {
  @Override
  public boolean value(Object o) {
    // One time there will be meaningful condition
    return false;
  }
}
