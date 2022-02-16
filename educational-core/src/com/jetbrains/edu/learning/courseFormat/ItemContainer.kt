package com.jetbrains.edu.learning.courseFormat;

import com.jetbrains.edu.learning.EduUtils;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ItemContainer extends StudyItem {
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

  public void addItem(StudyItem item) {
    items.add(item);
  }

  public void removeItem(StudyItem item) {
    items.remove(item);
  }

  public void sortItems() {
    Collections.sort(items, EduUtils.INDEX_COMPARATOR);
  }
}
