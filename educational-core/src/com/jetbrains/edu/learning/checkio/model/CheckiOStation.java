package com.jetbrains.edu.learning.checkio.model;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CheckiOStation {
  private final int id;
  private final String name;
  private List<CheckiOMission> missions;

  public CheckiOStation(int id, String name) {
    this.id = id;
    this.name = name;
    this.missions = new ArrayList<>();
  }

  public void addMissions(@NotNull List<CheckiOMission> missions) {
    this.missions.addAll(missions);
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @NotNull
  public List<CheckiOMission> getMissions() {
    return missions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CheckiOStation station = (CheckiOStation)o;
    return id == station.id &&
           Objects.equals(name, station.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }
}
