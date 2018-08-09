package com.jetbrains.edu.learning.checkio.api.wrappers;

import com.google.gson.annotations.SerializedName;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CheckiOMissionListWrapper implements ResponseWrapper<List<CheckiOMission>> {
  @SerializedName("objects") private List<CheckiOMissionWrapper> missions = new ArrayList<>();

  @Override
  public List<CheckiOMission> unwrap() {
    return missions.stream().map(CheckiOMissionWrapper::unwrap).collect(Collectors.toList());
  }
}
