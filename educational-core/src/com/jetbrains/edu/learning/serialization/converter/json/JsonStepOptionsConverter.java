package com.jetbrains.edu.learning.serialization.converter.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;

public interface JsonStepOptionsConverter {
  @NotNull
  ObjectNode convert(@NotNull ObjectNode stepOptionsJson);
}
