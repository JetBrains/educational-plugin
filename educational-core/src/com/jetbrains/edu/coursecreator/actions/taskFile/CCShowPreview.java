/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.jetbrains.edu.coursecreator.actions.taskFile;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.impl.util.LabeledEditor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.PathUtil;
import com.intellij.util.ui.JBUI;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.VirtualFileExt;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException;
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.placeholder.PlaceholderHighlightingManager;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static com.intellij.openapi.ui.Messages.showInfoMessage;

@SuppressWarnings("ComponentNotRegistered")  // educational-core.xml
public class CCShowPreview extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(CCShowPreview.class.getName());

  @NonNls
  public static final String ACTION_ID = "Educational.Educator.ShowPreview";

  public CCShowPreview() {
    super(EduCoreBundle.lazyMessage("action.show.preview.text"), EduCoreBundle.lazyMessage("action.show.preview.description"), null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(false);
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    if (!CCUtils.isCourseCreator(project)) {
      return;
    }
    final PsiFile file = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
    if (file != null) {
      TaskFile taskFile = VirtualFileExt.getTaskFile(file.getVirtualFile(), project);
      if (taskFile != null && taskFile.isVisible()) {
        presentation.setEnabledAndVisible(true);
      }
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    Module module = LangDataKeys.MODULE.getData(e.getDataContext());
    if (project == null || module == null) {
      return;
    }
    final PsiFile file = CommonDataKeys.PSI_FILE.getData(e.getDataContext());
    if (file == null) {
      return;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    VirtualFile virtualFile = file.getVirtualFile();
    TaskFile taskFile = VirtualFileExt.getTaskFile(virtualFile, project);
    if (taskFile == null || !taskFile.isVisible()) {
      return;
    }
    final PsiDirectory taskDir = file.getContainingDirectory();
    if (taskDir == null) {
      return;
    }
    PsiDirectory lessonDir = taskDir.getParentDirectory();
    if (lessonDir == null) {
      return;
    }

    if (taskFile.getAnswerPlaceholders().isEmpty()) {
      showInfoMessage(EduCoreBundle.message("dialog.message.no.preview.for.file"),
                      EduCoreBundle.message("dialog.title.no.preview.for.file"));
      return;
    }

    final Task task = taskFile.getTask();
    ApplicationManager.getApplication().runWriteAction(() -> {
      TaskFile studentTaskFile;
      try {
        studentTaskFile = VirtualFileExt.toStudentFile(virtualFile, project, task);
      }
      catch (BrokenPlaceholderException exception) {
        LOG.info("Failed to Create Preview: " + exception.getMessage());
        showErrorDialog(exception.getPlaceholderInfo(), EduCoreBundle.message("dialog.title.failed.to.create.preview"));
        return;
      }
      catch (HugeBinaryFileException exception) {
        LOG.info("Failed to Create Preview: " + exception.getMessage());
        showErrorDialog(exception.getMessage(), EduCoreBundle.message("dialog.title.failed.to.create.preview"));
        return;
      }
      if (studentTaskFile != null) {
        showPreviewDialog(project, studentTaskFile);
      }
    });
    EduCounterUsageCollector.previewTaskFile();
  }

  private static void showPreviewDialog(@NotNull Project project, @NotNull TaskFile taskFile) {
    final FrameWrapper showPreviewFrame = new FrameWrapper(project);
    final LightVirtualFile userFile = new LightVirtualFile(PathUtil.getFileName(taskFile.getName()), taskFile.getText());
    showPreviewFrame.setTitle(userFile.getName());
    LabeledEditor labeledEditor = new LabeledEditor(null);
    final EditorFactory factory = EditorFactory.getInstance();
    Document document = FileDocumentManager.getInstance().getDocument(userFile);
    if (document == null) {
      return;
    }
    final EditorEx createdEditor = (EditorEx)factory.createEditor(document, project, userFile, true);
    Disposer.register(StudyTaskManager.getInstance(project), () -> factory.releaseEditor(createdEditor));
    PlaceholderHighlightingManager.showPlaceholders(project, taskFile, createdEditor);
    JPanel header = new JPanel();
    header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
    header.setBorder(JBUI.Borders.empty(10));
    header.add(new JLabel(EduCoreBundle.message("ui.label.read.only.preview")));
    String timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
    header.add(new JLabel(EduCoreBundle.message("ui.label.created.at", timeStamp)));
    JComponent editorComponent = createdEditor.getComponent();
    labeledEditor.setComponent(editorComponent, header);
    createdEditor.setCaretVisible(false);
    createdEditor.setCaretEnabled(false);
    showPreviewFrame.setComponent(labeledEditor);
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      showPreviewFrame.setSize(new Dimension(500, 500));
      showPreviewFrame.show();
    }
  }
}
