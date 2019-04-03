package com.jetbrains.edu.learning.courseFormat;

import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation;
import com.jetbrains.edu.learning.courseFormat.tasks.*;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ItemContainer extends StudyItem {
  @SuppressWarnings("deprecation")
  @AbstractCollection(elementTypes = {
    Section.class,
    //lessons:
    Lesson.class,
    FrameworkLesson.class,
    //tasks:
    CheckiOStation.class,
    CheckiOMission.class,
    EduTask.class,
    ChoiceTask.class,
    TheoryTask.class,
    CodeTask.class,
    OutputTask.class,
    IdeTask.class
  })
  protected List<StudyItem> items = new ArrayList<>();

  @Nullable
  public StudyItem getItem(@NotNull final String name) {
    return StreamEx.of(items).findFirst(item -> item.getName().equals(name)).orElse(null);
  }

  @NotNull
  public List<StudyItem> getItems() {
    return Collections.unmodifiableList(items);
  }

  public void setItems(List<StudyItem> items) {
    this.items = items;
  }

  public void sortItems() {
    Collections.sort(items, EduUtils.INDEX_COMPARATOR);
  }
}
