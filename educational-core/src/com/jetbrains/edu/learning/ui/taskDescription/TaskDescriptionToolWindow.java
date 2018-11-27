/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.ui.taskDescription;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.codeInsight.documentation.DocumentationManagerProtocol;
import com.intellij.ide.actions.QualifiedNameProvider;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;

public abstract class TaskDescriptionToolWindow {
  public static final String EMPTY_TASK_TEXT = "Please, open any task to see task description";
  public static final String PSI_ELEMENT_PROTOCOL = DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL;


  public TaskDescriptionToolWindow() {
    LafManager.getInstance().addLafManagerListener(new StudyLafManagerListener());
  }

  public abstract JComponent createTaskInfoPanel(@NotNull Project project);

  public abstract JComponent createTaskSpecificPanel(Task currentTask);

  public void updateTaskSpecificPanel(@Nullable Task task) {
  }

  protected String wrapHints(@NotNull String text) {
    Document document = Jsoup.parse(text);
    Elements hints = document.getElementsByClass("hint");
    for (int i = 0; i < hints.size(); i++) {
      Element hint = hints.get(i);
      String hintText = wrapHint(hint.html(), i + 1);
      hint.html(hintText);
    }
    return document.html();
  }

  protected abstract String wrapHint(@NotNull String hintText, int hintNumber);

  protected void setTaskText(@NotNull Project project, @Nullable Task task) {
    setText(getTaskDescriptionWithCodeHighlighting(project, task));
  }

  public abstract void setText(@NotNull String text);

  protected abstract void updateLaf();

  @VisibleForTesting
  @NotNull
  public static String getTaskDescriptionWithCodeHighlighting(@NotNull Project project, @Nullable Task task) {
    if (task != null) {
      String taskText = EduUtils.getTaskTextFromTask(task.getTaskDir(project), task);
      if (taskText != null) {
        return EduCodeHighlighter.highlightCodeFragments(project, taskText, task.getCourse().getLanguageById());
      }
    }
    return EMPTY_TASK_TEXT;
  }

  public static void navigateToPsiElement(@NotNull Project project, @NotNull String url) {
    String qualifiedName = url.replace(PSI_ELEMENT_PROTOCOL, "");

    Application application = ApplicationManager.getApplication();
    application.invokeLater(() -> application.runReadAction(() -> {
      for (QualifiedNameProvider provider : Extensions.getExtensions(QualifiedNameProvider.EP_NAME)) {
        PsiElement element = provider.qualifiedNameToElement(qualifiedName, project);
        if (element instanceof NavigatablePsiElement) {
          NavigatablePsiElement navigatableElement = (NavigatablePsiElement)element;
          if (navigatableElement.canNavigate()) {
            navigatableElement.navigate(true);
          }
          break;
        }
      }
    }));
  }

  private class StudyLafManagerListener implements LafManagerListener {
    @Override
    public void lookAndFeelChanged(@NotNull LafManager manager) {
      updateLaf();
    }
  }
}
