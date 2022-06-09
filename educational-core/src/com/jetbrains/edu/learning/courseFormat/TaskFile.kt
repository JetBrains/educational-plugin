package com.jetbrains.edu.learning.courseFormat;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.edu.coursecreator.stepik.StepikChangeRetriever;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of task file which contains task answer placeholders for student to type in and
 * which is visible to student in project view
 * <p>
 * Update {@link StepikChangeRetriever#isEqualTo(TaskFile, TaskFile)} if you added new property that has to be compared
 */

public class TaskFile {
  private String myName;
  private boolean myTrackChanges = true;
  private boolean myHighlightErrors = false;
  private List<AnswerPlaceholder> myAnswerPlaceholders = new ArrayList<>();
  private boolean myVisible = true;
  // Should be used only in student mode
  private boolean myLearnerCreated = false;
  private boolean myEditable = true;

  private String myText = "";

  transient private Task myTask;

  public static final Logger LOG = Logger.getInstance(TaskFile.class);

  public TaskFile() {
  }

  public TaskFile(@NotNull String name, @NotNull String text) {
    myName = name;
    setText(text);
  }

  public TaskFile(@NotNull String name, @NotNull String text, boolean isVisible) {
    this(name, text);
    myVisible = isVisible;
  }

  public TaskFile(@NotNull String name, @NotNull String text, boolean isVisible, boolean isLearnerCreated) {
    this(name, text, isVisible);
    myLearnerCreated = isLearnerCreated;
  }

  public void initTaskFile(final Task task, boolean isRestarted) {
    setTask(task);
    final List<AnswerPlaceholder> answerPlaceholders = getAnswerPlaceholders();
    for (AnswerPlaceholder answerPlaceholder : answerPlaceholders) {
      answerPlaceholder.initAnswerPlaceholder(this, isRestarted);
    }
    answerPlaceholders.sort(AnswerPlaceholderComparator.INSTANCE);
    for (int i = 0; i < answerPlaceholders.size(); i++) {
      answerPlaceholders.get(i).setIndex(i);
    }
  }

  public List<AnswerPlaceholder> getAnswerPlaceholders() {
    return myAnswerPlaceholders;
  }

  public void setAnswerPlaceholders(List<AnswerPlaceholder> answerPlaceholders) {
    this.myAnswerPlaceholders = answerPlaceholders;
  }

  public void addAnswerPlaceholder(AnswerPlaceholder answerPlaceholder) {
    myAnswerPlaceholders.add(answerPlaceholder);
  }

  public Task getTask() {
    return myTask;
  }

  public void setTask(Task task) {
    myTask = task;
  }

  /**
   * @param offset position in editor
   * @return answer placeholder located in specified position or null if there is no task window in this position
   */
  @Nullable
  public AnswerPlaceholder getAnswerPlaceholder(int offset) {
    for (AnswerPlaceholder placeholder : getAnswerPlaceholders()) {
      int placeholderStart = placeholder.getOffset();
      int placeholderEnd = placeholder.getEndOffset();
      if (placeholderStart <= offset && offset <= placeholderEnd) {
        return placeholder;
      }
    }
    return null;
  }

  public boolean isTrackChanges() {
    return myTrackChanges;
  }

  public void setTrackChanges(boolean trackChanges) {
    myTrackChanges = trackChanges;
  }

  public boolean isHighlightErrors() {
    return myHighlightErrors;
  }

  public void setHighlightErrors(boolean highlightErrors) {
    myHighlightErrors = highlightErrors;
  }

  public String getName() {
    return myName;
  }

  public void setName(String name) {
    myName = name;
  }

  public boolean isVisible() {
    return myVisible;
  }

  public boolean isEditable() {
    return myEditable;
  }

  public void setEditable(boolean editable) {
    myEditable = editable;
  }

  public void setVisible(boolean visible) {
    myVisible = visible;
  }

  public boolean isLearnerCreated() {
    return myLearnerCreated;
  }

  public void setLearnerCreated(boolean learnerCreated) {
    myLearnerCreated = learnerCreated;
  }

  @NotNull
  public String getText() {
    return myText;
  }

  public void setText(@Nullable String text) {
    myText = StringUtil.notNullize(text);
  }

  @SuppressWarnings("unused") // used for yaml serialization
  @Nullable
  public String getTextToSerialize() {
    if (myTask.getLesson() instanceof FrameworkLesson && OpenApiExtKt.toEncodeFileContent(myName)) {
      if (EduUtils.exceedsBase64ContentLimit(myText)) {
        LOG.warn(String.format("Base64 encoding of `%s` file exceeds limit (%s), its content isn't serialized",
                               myName, StringUtil.formatFileSize(EduUtils.getBinaryFileLimit())));
        return null;
      }
      return myText;
    }

    String extension = FileUtilRt.getExtension(myName);
    FileType fileType = FileTypeManagerEx.getInstanceEx().getFileTypeByExtension(extension);
    if (fileType.isBinary()) {
      return null;
    }

    return myText;
  }

  public void sortAnswerPlaceholders() {
    myAnswerPlaceholders.sort(AnswerPlaceholderComparator.INSTANCE);
    for (int i = 0; i < myAnswerPlaceholders.size(); i++) {
      myAnswerPlaceholders.get(i).setIndex(i);
    }
  }

  public boolean hasFailedPlaceholders() {
    for (AnswerPlaceholder placeholder : myAnswerPlaceholders) {
      if (placeholder.getStatus() == CheckStatus.Failed) {
        return true;
      }
    }
    return false;
  }

  public boolean isValid(@NotNull String text) {
    int length = text.length();
    List<AnswerPlaceholder> placeholders = getAnswerPlaceholders();
    for (AnswerPlaceholder placeholder : placeholders) {
      if (!placeholder.isValid(length)) return false;
    }
    return true;
  }
}
