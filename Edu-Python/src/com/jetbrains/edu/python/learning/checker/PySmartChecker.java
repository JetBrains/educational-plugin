package com.jetbrains.edu.python.learning.checker;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduDocumentListener;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.checker.TestsOutputParser;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

class PySmartChecker {
  private PySmartChecker() {

  }

  private static final Logger LOG = Logger.getInstance(PySmartChecker.class);

  private static void smartCheck(@NotNull final AnswerPlaceholder placeholder,
                                @NotNull final Project project,
                                @NotNull final VirtualFile answerFile,
                                @NotNull final TaskFile answerTaskFile,
                                @NotNull final TaskFile usersTaskFile,
                                @NotNull final PyTestRunner testRunner,
                                @NotNull final VirtualFile virtualFile,
                                @NotNull final Document usersDocument) {
    VirtualFile fileWindows = null;
    VirtualFile windowCopy = null;
    try {
      final int index = placeholder.getIndex();
      String windowCopyName = answerFile.getNameWithoutExtension() + index + EduNames.WINDOW_POSTFIX + answerFile.getExtension();
      windowCopy = answerFile.copy(project, answerFile.getParent(), windowCopyName);
      final FileDocumentManager documentManager = FileDocumentManager.getInstance();
      final Document windowDocument = documentManager.getDocument(windowCopy);
      if (windowDocument != null) {
        TaskFile windowTaskFile = answerTaskFile.getTask().copy().getTaskFile(EduUtils.pathRelativeToTask(project, virtualFile));
        if (windowTaskFile == null) {
          return;
        }
        final AnswerPlaceholder userAnswerPlaceholder = usersTaskFile.getAnswerPlaceholders().get(placeholder.getIndex());

        EduDocumentListener.runWithListener(project, windowTaskFile, windowCopy, document -> {
          int start = placeholder.getOffset();
          int end = placeholder.getEndOffset();
          int userStart = userAnswerPlaceholder.getOffset();
          int userEnd = userAnswerPlaceholder.getEndOffset();
          String text = usersDocument.getText(new TextRange(userStart, userEnd));
          document.replaceString(start, end, text);
          ApplicationManager.getApplication().runWriteAction(() -> documentManager.saveDocument(document));
          return Unit.INSTANCE;
        });

        fileWindows = EduUtils.flushWindows(windowTaskFile, windowCopy);
        Process smartTestProcess = testRunner.createCheckProcess(project, windowCopy.getPath());
        final CapturingProcessHandler handler = new CapturingProcessHandler(smartTestProcess, null, windowCopy.getPath());
        final ProcessOutput output = handler.runProcess();
        final Course course = StudyTaskManager.getInstance(project).getCourse();
        if (course != null) {
          final CheckStatus status = new TestsOutputParser().getCheckResult(output.getStdoutLines()).getStatus();
          userAnswerPlaceholder.setStatus(status);
          YamlFormatSynchronizer.saveItem(usersTaskFile.getTask());
        }
      }
    }
    catch (ExecutionException | IOException e) {
      LOG.error(e);
    }
    finally {
      EduUtils.deleteFile(windowCopy);
      EduUtils.deleteFile(fileWindows);
    }
  }

  static void runSmartTestProcess(@NotNull final VirtualFile taskDir,
                                  @NotNull final PyTestRunner testRunner,
                                  @NotNull final TaskFile taskFile,
                                  @NotNull final Project project) {
    final VirtualFile virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir);
    if (virtualFile == null) {
      return;
    }
    Pair<VirtualFile, TaskFile> pair = getCopyWithAnswers(project, taskDir, virtualFile, taskFile);
    if (pair == null) {
      return;
    }
    VirtualFile answerFile = pair.getFirst();
    TaskFile answerTaskFile = pair.getSecond();
    try {
      for (final AnswerPlaceholder answerPlaceholder : answerTaskFile.getAnswerPlaceholders()) {
        final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) {
          continue;
        }
        smartCheck(answerPlaceholder, project, answerFile, answerTaskFile, taskFile, testRunner,
                   virtualFile, document);
      }
    }
    finally {
      EduUtils.deleteFile(answerFile);
    }
  }

  static Pair<VirtualFile, TaskFile> getCopyWithAnswers(@NotNull Project project,
                                                        @NotNull final VirtualFile taskDir,
                                                        @NotNull final VirtualFile file,
                                                        @NotNull final TaskFile source) {
    try {
      VirtualFile answerFile = file.copy(taskDir, taskDir, file.getNameWithoutExtension() + EduNames.ANSWERS_POSTFIX + "." + file.getExtension());
      TaskFile answerTaskFile = source.getTask().copy().getTaskFile(EduUtils.pathRelativeToTask(project, file));
      if (answerTaskFile == null) {
        return null;
      }

      EduDocumentListener.runWithListener(project, answerTaskFile, answerFile, (document -> {
        for (AnswerPlaceholder answerPlaceholder : answerTaskFile.getAnswerPlaceholders()) {
          final int start = answerPlaceholder.getOffset();
          final int end = answerPlaceholder.getEndOffset();
          final String text = answerPlaceholder.getPossibleAnswer();
          document.replaceString(start, end, text);
        }
        final FileDocumentManager documentManager = FileDocumentManager.getInstance();
        ApplicationManager.getApplication().runWriteAction(() -> documentManager.saveDocument(document));
        return Unit.INSTANCE;
      }));
      return Pair.create(answerFile, answerTaskFile);
    }
    catch (IOException e) {
      LOG.error(e);
    }
    return null;
  }
}
