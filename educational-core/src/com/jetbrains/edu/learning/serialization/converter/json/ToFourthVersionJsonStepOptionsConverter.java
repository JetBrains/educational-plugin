package com.jetbrains.edu.learning.serialization.converter.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.Json.TITLE;

public class ToFourthVersionJsonStepOptionsConverter implements JsonStepOptionsConverter {

  @NotNull
  @Override
  public ObjectNode convert(@NotNull ObjectNode stepOptionsJson) {
    if (stepOptionsJson.has(TITLE) &&
        StepikNames.PYCHARM_ADDITIONAL.equals(stepOptionsJson.get(TITLE).asText())) {
      stepOptionsJson.remove(TITLE);
      stepOptionsJson.put(TITLE, EduNames.ADDITIONAL_MATERIALS);
    }
    return stepOptionsJson;
  }
}
