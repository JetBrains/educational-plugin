package com.jetbrains.edu.learning.checkio.model;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CheckiOMissionListWrapper {
  @SerializedName("objects")
  private List<CheckiOMissionWrapper> missions;

  private CheckiOMissionListWrapper() {
    missions = new ArrayList<>();
  }

  @NotNull
  public List<CheckiOMissionWrapper> getMissions() {
    return missions;
  }
}
