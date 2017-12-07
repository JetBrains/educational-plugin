package com.jetbrains.edu.learning.serialization.converter.json;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public interface JsonStepOptionsConverter {
  @NotNull
  JsonObject convert(@NotNull JsonObject stepOptionsJson);
}
