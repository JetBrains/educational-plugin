package com.jetbrains.edu.learning.checkio.courseFormat;

import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class CheckiOStation extends Lesson {
  public CheckiOStation() {}

  public void addMission(@NotNull CheckiOMission mission) {
    addTask(mission);
  }

  @Nullable
  public CheckiOMission getMission(int id) {
    final Task task = getTask(id);
    return (task instanceof CheckiOMission ? (CheckiOMission)task : null);
  }

  @NotNull
  public List<CheckiOMission> getMissions() {
    return getTaskList().stream().filter(CheckiOMission.class::isInstance).map(CheckiOMission.class::cast).collect(Collectors.toList());
  }

  @Override
  public String getItemType() {
    return "checkiO";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CheckiOStation station = (CheckiOStation)o;
    return getId() == station.getId();
  }

  @Override
  public int hashCode() {
    return getId();
  }

  @Override
  public String toString() {
    return "missions=[" + getMissions().stream().map(CheckiOMission::toString).collect(Collectors.joining("\n")) + "]";
  }
}
