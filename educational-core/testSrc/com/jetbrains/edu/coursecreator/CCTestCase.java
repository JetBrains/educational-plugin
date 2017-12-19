package com.jetbrains.edu.coursecreator;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.ComparisonFailure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class CCTestCase extends LightPlatformCodeInsightFixtureTestCase {
  private static final Logger LOG = Logger.getInstance(CCTestCase.class);

  @Nullable
  public static RangeHighlighter getHighlighter(MarkupModel model, AnswerPlaceholder placeholder) {
    for (RangeHighlighter highlighter : model.getAllHighlighters()) {
      int endOffset = placeholder.getOffset() + placeholder.getRealLength();
      if (highlighter.getStartOffset() == placeholder.getOffset() && highlighter.getEndOffset() == endOffset) {
        return highlighter;
      }
    }
    return null;
  }

  protected static void checkHighlighters(TaskFile taskFile, MarkupModel markupModel) {
    for (AnswerPlaceholder answerPlaceholder : taskFile.getActivePlaceholders()) {
      if (getHighlighter(markupModel, answerPlaceholder) == null) {
        throw new AssertionError("No highlighter for placeholder: " + CCTestsUtil.getPlaceholderPresentation(answerPlaceholder));
      }
    }
  }

  public void checkByFile(TaskFile taskFile, String fileName, boolean useLength) {
    Pair<Document, List<AnswerPlaceholder>> placeholders = getPlaceholders(fileName, useLength, true);
    String message = "Placeholders don't match";
    if (taskFile.getActivePlaceholders().size() != placeholders.second.size()) {
      throw new ComparisonFailure(message,
                                  CCTestsUtil.getPlaceholdersPresentation(taskFile.getActivePlaceholders()),
                                  CCTestsUtil.getPlaceholdersPresentation(placeholders.second));
    }
    for (AnswerPlaceholder answerPlaceholder : placeholders.getSecond()) {
      AnswerPlaceholder placeholder = taskFile.getAnswerPlaceholder(answerPlaceholder.getOffset());
      if (!CCTestsUtil.comparePlaceholders(placeholder, answerPlaceholder)) {
        throw new ComparisonFailure(message,
                                    CCTestsUtil.getPlaceholdersPresentation(taskFile.getActivePlaceholders()),
                                    CCTestsUtil.getPlaceholdersPresentation(placeholders.second));
      }
    }
  }

  @Override
  protected String getBasePath() {
    return new File("testData").getAbsolutePath().replace(File.separatorChar, '/');
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Course course = new Course();
    course.setName("test course");
    course.setLanguage(PlainTextLanguage.INSTANCE.getID());
    StudyTaskManager.getInstance(getProject()).setCourse(course);

    Lesson lesson = new Lesson();
    lesson.setName("lesson1");
    Task task = new EduTask();
    task.setName("task1");
    task.setIndex(1);
    lesson.addTask(task);
    lesson.setIndex(1);
    course.addLesson(lesson);
    course.setCourseMode(CCUtils.COURSE_MODE);
    course.initCourse(false);
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          VirtualFile lesson1 = myFixture.getProject().getBaseDir().createChildDirectory(this, "lesson1");
          lesson1.createChildDirectory(this, "task1");
        }
        catch (IOException e) {
          //ignore
        }
      }
    });
  }

  protected VirtualFile copyFileToTask(String name) {
    return myFixture.copyFileToProject(name, "lesson1/task1/" + name);
  }

  protected VirtualFile configureByTaskFile(String name) {
    Task task = StudyTaskManager.getInstance(getProject()).getCourse().getLessons().get(0).getTaskList().get(0);
    TaskFile taskFile = new TaskFile();
    taskFile.setTask(task);
    task.getTaskFiles().put(name, taskFile);
    VirtualFile file = copyFileToTask(name);

    taskFile.name = name;
    myFixture.configureFromExistingVirtualFile(file);
    Document document = FileDocumentManager.getInstance().getDocument(file);
    for (AnswerPlaceholder placeholder : getPlaceholders(document, false)) {
      taskFile.addAnswerPlaceholder(placeholder);
    }
    taskFile.sortAnswerPlaceholders();
    EduUtils.drawAllAnswerPlaceholders(myFixture.getEditor(), taskFile);
    return file;
  }

  public static List<AnswerPlaceholder> getPlaceholders(Document document, boolean useLength) {
    final List<AnswerPlaceholder> placeholders = new ArrayList<>();
    new WriteCommandAction(null) {
      @Override
      protected void run(@NotNull Result result) {
        final String openingTagRx = "<placeholder( taskText=\"(.+?)\")?( possibleAnswer=\"(.+?)\")?( hint=\"(.+?)\")?( hint2=\"(.+?)\")?>";
        final String closingTagRx = "</placeholder>";
        CharSequence text = document.getCharsSequence();
        final Matcher openingMatcher = Pattern.compile(openingTagRx).matcher(text);
        final Matcher closingMatcher = Pattern.compile(closingTagRx).matcher(text);
        int pos = 0;
        while (openingMatcher.find(pos)) {
          AnswerPlaceholder answerPlaceholder = new AnswerPlaceholder();
          AnswerPlaceholderSubtaskInfo subtaskInfo = new AnswerPlaceholderSubtaskInfo();
          answerPlaceholder.getSubtaskInfos().put(0, subtaskInfo);
          answerPlaceholder.setUseLength(useLength);
          String taskText = openingMatcher.group(2);
          if (taskText != null) {
            answerPlaceholder.setTaskText(taskText);
            answerPlaceholder.setLength(taskText.length());
          }
          String possibleAnswer = openingMatcher.group(4);
          if (possibleAnswer != null) {
            answerPlaceholder.setPossibleAnswer(possibleAnswer);
          }
          final ArrayList<String> hints = ContainerUtil.newArrayList();
          String hint = openingMatcher.group(6);
          if (hint != null) {
            hints.add(hint);
          }
          String hint2 = openingMatcher.group(8);
          if (hint2 != null) {
            hints.add(hint2);
          }
          if (!hints.isEmpty()) {
            answerPlaceholder.setHints(hints);
          }
          answerPlaceholder.setOffset(openingMatcher.start());
          if (!closingMatcher.find(openingMatcher.end())) {
            LOG.error("No matching closing tag found");
          }
          if (useLength) {
            answerPlaceholder.setTaskText(String.valueOf(text.subSequence(openingMatcher.end(), closingMatcher.start())));
            answerPlaceholder.setLength(closingMatcher.start() - openingMatcher.end());
          } else {
            if (possibleAnswer == null) {
              answerPlaceholder.setPossibleAnswer(document.getText(TextRange.create(openingMatcher.end(), closingMatcher.start())));
            }
          }
          document.deleteString(closingMatcher.start(), closingMatcher.end());
          document.deleteString(openingMatcher.start(), openingMatcher.end());
          FileDocumentManager.getInstance().saveDocument(document);
          placeholders.add(answerPlaceholder);
          pos = answerPlaceholder.getOffset() + answerPlaceholder.getRealLength();
        }
      }
    }.execute();
    return placeholders;
  }

  public Pair<Document, List<AnswerPlaceholder>> getPlaceholders(String name) {
    return getPlaceholders(name, true, false);
  }

  public Pair<Document, List<AnswerPlaceholder>> getPlaceholders(String name, boolean useLength, boolean removeMarkers) {
    try {
      String text = StringUtil.convertLineSeparators(FileUtil.loadFile(new File(getBasePath(), name)));
      Document tempDocument = EditorFactory.getInstance().createDocument(text);
      if (removeMarkers) {
        EditorTestUtil.extractCaretAndSelectionMarkers(tempDocument);
      }
      List<AnswerPlaceholder> placeholders = getPlaceholders(tempDocument, useLength);
      return Pair.create(tempDocument, placeholders);
    }
    catch (IOException e) {
      LOG.error(e);
    }
    return Pair.create(null, null);
  }

  @NotNull
  protected Presentation testAction(DataContext context, AnAction action) {
    TestActionEvent e = new TestActionEvent(context, action);
    action.beforeActionPerformedUpdate(e);
    if (e.getPresentation().isEnabledAndVisible()) {
      action.actionPerformed(e);
    }
    return e.getPresentation();
  }

  @NotNull
  @Override
  protected String getTestDataPath() {
    return getBasePath();
  }
}


