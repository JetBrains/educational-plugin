package com.jetbrains.edu.learning.courseFormat;

import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.jetbrains.edu.coursecreator.stepik.StepikChangeRetriever;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Implementation of answer placeholders which user should type in
 * <p>
 * Update {@link StepikChangeRetriever#isEqualTo(AnswerPlaceholder, AnswerPlaceholder)} if you added new property that has to be compared
 */

public class AnswerPlaceholder {

  private int myOffset = -1;
  /*
   * length of text to surround with visual placeholder
   * (placeholderText.length in student file; possibleAnswer.length in course creator file)
   */
  private int myLength = -1;
  private int myIndex = -1;
  private MyInitialState myInitialState;
  @Nullable
  private AnswerPlaceholderDependency myPlaceholderDependency = null;
  private boolean myIsInitializedFromDependency = false;
  private String myPossibleAnswer = "";  // could be empty in course creator file
  private String myPlaceholderText;     //  could be empty in student file (including task file preview in course creator mode)
  private boolean mySelected = false;
  private CheckStatus myStatus = CheckStatus.Unchecked;

  transient private TaskFile myTaskFile;

  /*
   * Actual student's answer, used to restore state of task of framework lesson after navigation actions
   */
  @Nullable
  private String myStudentAnswer = null;

  public AnswerPlaceholder() {
  }

  public AnswerPlaceholder(int offset, String placeholderText) {
    this.myOffset = offset;
    this.myLength = placeholderText.length();
    this.myPlaceholderText = placeholderText;
  }

  public void initAnswerPlaceholder(final TaskFile file, boolean isRestarted) {
    setTaskFile(file);
    if (!isRestarted) {
      if (myPlaceholderDependency != null) {
        myPlaceholderDependency.setAnswerPlaceholder(this);
      }
      setInitialState(new MyInitialState(myOffset, myLength));
      myStatus = file.getTask().getStatus();
    }
  }

  public int getIndex() {
    return myIndex;
  }

  public void setIndex(int index) {
    myIndex = index;
  }

  public int getLength() {
    return myLength;
  }

  public void setLength(int length) {
    myLength = length;
  }

  public String getPossibleAnswer() {
    return myPossibleAnswer;
  }

  public void setPossibleAnswer(String possibleAnswer) {
    myPossibleAnswer = possibleAnswer;
  }

  public MyInitialState getInitialState() {
    return myInitialState;
  }

  public void setInitialState(MyInitialState initialState) {
    myInitialState = initialState;
  }

  public String getPlaceholderText() {
    return myPlaceholderText;
  }

  public void setPlaceholderText(String placeholderText) {
    myPlaceholderText = placeholderText;
  }

  public TaskFile getTaskFile() {
    return myTaskFile;
  }

  public void setTaskFile(TaskFile taskFile) {
    myTaskFile = taskFile;
  }

  public int getPossibleAnswerLength() {
    return getPossibleAnswer().length();
  }

  /**
   * Returns window to its initial state
   */
  public void reset(boolean revertStartOffset) {
    if (revertStartOffset) {
      myOffset = myInitialState.getOffset();
    }
    myLength = myInitialState.getLength();
    myStatus = CheckStatus.Unchecked;
    myIsInitializedFromDependency = false;
  }

  public CheckStatus getStatus() {
    return myStatus;
  }

  public void setStatus(CheckStatus status) {
    myStatus = status;
  }

  public boolean getSelected() {
    return mySelected;
  }

  public void setSelected(boolean selected) {
    mySelected = selected;
  }

  public void init() {
    setInitialState(new MyInitialState(myOffset, getPlaceholderText().length()));
  }

  public int getOffset() {
    return myOffset;
  }

  public void setOffset(int offset) {
    myOffset = offset;
  }

  @Nullable
  public String getStudentAnswer() {
    return myStudentAnswer;
  }

  public void setStudentAnswer(@Nullable String studentAnswer) {
    myStudentAnswer = studentAnswer;
  }

  public boolean isVisible() {
    return myPlaceholderDependency == null || myPlaceholderDependency.isVisible() || !isInitializedFromDependency();
  }

  public static class MyInitialState {
    private int length = -1;
    private int offset = -1;

    @SuppressWarnings("unused") // used for deserialization
    public MyInitialState() { }

    public MyInitialState(int initialOffset, int length) {
      this.offset = initialOffset;
      this.length = length;
    }

    public int getLength() {
      return length;
    }

    public void setLength(int length) {
      this.length = length;
    }

    public int getOffset() {
      return offset;
    }

    public void setOffset(int offset) {
      this.offset = offset;
    }
  }

  boolean isValid(int textLength) {
    int end = getEndOffset();
    return getOffset() >= 0 && getLength() >= 0 && end <= textLength;
  }

  @Nullable
  public AnswerPlaceholderDependency getPlaceholderDependency() {
    return myPlaceholderDependency;
  }

  public void setPlaceholderDependency(@Nullable AnswerPlaceholderDependency placeholderDependency) {
    myPlaceholderDependency = placeholderDependency;
    if (placeholderDependency != null) {
      myPlaceholderDependency.setAnswerPlaceholder(this);
    }
  }

  public boolean isInitializedFromDependency() {
    return myIsInitializedFromDependency;
  }

  public void setInitializedFromDependency(boolean initializedFromDependency) {
    myIsInitializedFromDependency = initializedFromDependency;
  }

  public int getEndOffset() {
    return myOffset + getLength();
  }

  @Override
  public String toString() {
    Task task = myTaskFile.getTask();
    Lesson lesson = task.getLesson();
    Section section = lesson.getSection();
    StringBuilder builder = new StringBuilder();
    if (section != null) {
      builder.append(section.getName());
      builder.append("#");
    }
    return builder.append(lesson.getName())
      .append("#")
      .append(task.getName())
      .append("#")
      .append(myTaskFile.getName())
      .append("[")
      .append(myOffset)
      .append(", ")
      .append(myOffset + myLength)
      .append("]")
      .toString();
  }

  @NotNull
  public JBColor getColor() {
    final CheckStatus status = getStatus();
    if (status == CheckStatus.Solved) {

      Color colorLight = ColorUtil.fromHex("26993D", JBColor.LIGHT_GRAY);
      colorLight = ColorUtil.toAlpha(colorLight, 90);

      Color colorDark = ColorUtil.fromHex("47CC5E", JBColor.LIGHT_GRAY);
      colorDark = ColorUtil.toAlpha(colorDark, 82);

      return new JBColor(colorLight, colorDark);
    }
    if (status == CheckStatus.Failed) {
      Color colorLight = ColorUtil.fromHex("CC0000", JBColor.GRAY);
      colorLight = ColorUtil.toAlpha(colorLight, 64);
      Color colorDark = ColorUtil.fromHex("FF7373", JBColor.GRAY);
      colorDark = ColorUtil.toAlpha(colorDark, 90);
      return new JBColor(colorLight, colorDark);
    }
    return getDefaultPlaceholderColor();
  }

  @NotNull
  public static JBColor getDefaultPlaceholderColor() {
    Color colorLight = ColorUtil.fromHex("284B73", JBColor.GRAY);
    colorLight = ColorUtil.toAlpha(colorLight, 64);
    Color colorDark = ColorUtil.fromHex("A1C1E6", JBColor.GRAY);
    colorDark = ColorUtil.toAlpha(colorDark, 72);
    return new JBColor(colorLight, colorDark);
  }
}
