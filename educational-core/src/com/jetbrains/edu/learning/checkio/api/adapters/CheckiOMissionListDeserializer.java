package com.jetbrains.edu.learning.checkio.api.adapters;

import com.google.gson.*;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CheckiOMissionListDeserializer implements JsonDeserializer<List<CheckiOMission>> {
  @Override
  public List<CheckiOMission> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    final Iterator<JsonElement> jsonMissionIterator = json.getAsJsonObject().get("objects").getAsJsonArray().iterator();


    final List<CheckiOMission> missionList = new ArrayList<>();

    jsonMissionIterator.forEachRemaining((jsonMission) -> {
      JsonObject jsonMissionObject = jsonMissionIterator.next().getAsJsonObject();

      CheckiOStation station = new CheckiOStation();
      station.setId(jsonMissionObject.get("stationId").getAsInt());
      station.setName(jsonMissionObject.get("stationName").getAsString());

      CheckiOMission mission = new CheckiOMission();
      mission.setStation(station);
      mission.setId(jsonMissionObject.get("id").getAsInt());
      mission.setName(jsonMissionObject.get("title").getAsString());
      mission.setDescriptionFormat(DescriptionFormat.HTML);
      mission.setDescriptionText(jsonMissionObject.get("description").getAsString());
      mission.setStatus(jsonMissionObject.get("isSolved").getAsBoolean() ? CheckStatus.Solved : CheckStatus.Unchecked);
      mission.setCode(jsonMissionObject.getAsJsonObject().get("code").getAsString());

      final JsonElement secondsPast = jsonMissionObject.get("secondsPast");

      // null value means that this mission hasn't been started on CheckiO yet,
      // in this case we should use local task file content after course updating,
      // so time from any local change must be less than from server
      mission.setSecondsFromLastChangeOnServer(secondsPast.isJsonNull() ? Long.MAX_VALUE : secondsPast.getAsLong());

      missionList.add(mission);
    });

    return missionList;
  }
}
