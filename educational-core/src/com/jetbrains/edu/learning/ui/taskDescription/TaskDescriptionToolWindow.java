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

import com.intellij.codeInsight.documentation.DocumentationManagerProtocol;
import com.intellij.ide.actions.QualifiedNameProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.ui.JBCardLayout;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.util.ui.JBUI;
import com.jetbrains.edu.coursecreator.actions.CCEditTaskDescription;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.editor.EduFileEditorManagerListener;
import com.jetbrains.edu.learning.stepik.StepikAdaptiveReactionsPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public abstract class TaskDescriptionToolWindow extends SimpleToolWindowPanel implements DataProvider, Disposable {

  private static final Logger LOG = Logger.getInstance(TaskDescriptionToolWindow.class);
  private static final String TASK_INFO_ID = "taskInfo";
  private static final String HELP_ID = "task.description";

  public static final String EMPTY_TASK_TEXT = "Please, open any task to see task description";
  public static final String PSI_ELEMENT_PROTOCOL = DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL;

  private final JBCardLayout myCardLayout;
  private final JPanel myContentPanel;
  private final OnePixelSplitter mySplitPane;

  private Task myCurrentTask = null;

  public TaskDescriptionToolWindow() {
    super(true, true);
    myCardLayout = new JBCardLayout();
    myContentPanel = new JPanel(myCardLayout);
    mySplitPane = new OnePixelSplitter(myVertical = true);
  }

  public void init(@NotNull final Project project) {
    final DefaultActionGroup group = getActionGroup(project);
    setActionToolbar(group);

    final JPanel panel = new JPanel(new BorderLayout());
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course != null && course.isAdaptive()) {
      panel.add(new StepikAdaptiveReactionsPanel(project), BorderLayout.NORTH);
    }

    JComponent taskInfoPanel = createTaskInfoPanel(project);
    panel.add(taskInfoPanel, BorderLayout.CENTER);

    myContentPanel.add(TASK_INFO_ID, panel);
    mySplitPane.setFirstComponent(myContentPanel);
    myCardLayout.show(myContentPanel, TASK_INFO_ID);

    setContent(mySplitPane);

    project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
                                                new EduFileEditorManagerListener(this, project));
    Task task = EduUtils.getCurrentTask(project);
    setCurrentTask(project, task);
  }

  public void setActionToolbar(DefaultActionGroup group) {
    JPanel toolbarPanel = createToolbarPanel(group);
    setToolbar(toolbarPanel);
  }

  public void dispose() {
  }

  //used in checkiO plugin.
  @SuppressWarnings("unused")
  public void showPanelById(@NotNull final String panelId) {
    myCardLayout.swipe(myContentPanel, panelId, JBCardLayout.SwipeDirection.AUTO);
  }

  public void setBottomComponent(JComponent component) {
    mySplitPane.setSecondComponent(component);
  }

  //used in checkiO plugin.
  @SuppressWarnings("unused")
  public JComponent getBottomComponent() {
    return mySplitPane.getSecondComponent();
  }

  //used in checkiO plugin.
  @SuppressWarnings("unused")
  public void setTopComponentPreferredSize(@NotNull final Dimension dimension) {
    myContentPanel.setPreferredSize(dimension);
  }

  //used in checkiO plugin.
  @SuppressWarnings("unused")
  public JPanel getContentPanel() {
    return myContentPanel;
  }


  public abstract JComponent createTaskInfoPanel(Project project);

  public static JPanel createToolbarPanel(ActionGroup group) {
    final ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("Study", group, true);
    return JBUI.Panels.simplePanel(actionToolBar.getComponent());
  }

  public static DefaultActionGroup getActionGroup(@NotNull final Project project) {
    DefaultActionGroup group = new DefaultActionGroup();
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      LOG.warn("Course is null");
      return group;
    }
    EduConfigurator configurator = EduConfiguratorManager.forLanguage(course.getLanguageById());
    if (configurator != null) {
      group.addAll(configurator.getTaskDescriptionActionGroup());
    }
    group.add(new CCEditTaskDescription());
    return group;
  }

  public abstract void setText(@NotNull String text);
  
  public void updateFonts(@NotNull Project project) {
    
  }

  public void updateTask(@NotNull Project project, @Nullable Task task) {
    if (myCurrentTask != task) {
      setCurrentTask(project, task);
    } else {
      setTaskText(project, myCurrentTask);
    }
  }

  public void setCurrentTask(@NotNull Project project, @Nullable Task task) {
    if (myCurrentTask != null && myCurrentTask == task) return;

    setTaskText(project, task);
    myCurrentTask = task;
  }

  private void setTaskText(@NotNull Project project, @Nullable Task task) {
    if (task != null) {
      String taskText = EduUtils.getTaskText(project);
      if (taskText != null) {
        setText(taskText);
      }
    }
    else {
      setText(EMPTY_TASK_TEXT);
    }
  }

  public void setEmptyText() {
    setText(EMPTY_TASK_TEXT);
  }

  @Nullable
  @Override
  public Object getData(String dataId) {
    if (PlatformDataKeys.HELP_ID.is(dataId)) {
      return HELP_ID;
    }
    return super.getData(dataId);
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
}
