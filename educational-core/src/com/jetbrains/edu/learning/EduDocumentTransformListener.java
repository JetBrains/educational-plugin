package com.jetbrains.edu.learning;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;

/**
 * Used in create answer file and create student file actions
 */
public class EduDocumentTransformListener extends EduDocumentListener {

  public EduDocumentTransformListener(Project project, TaskFile taskFile) {
    super(project, taskFile);
  }

  @Override
  protected void updatePlaceholder(@NotNull AnswerPlaceholder answerPlaceholder, @NotNull Document document, int start, int length) {
    answerPlaceholder.setOffset(start);
  }
}

