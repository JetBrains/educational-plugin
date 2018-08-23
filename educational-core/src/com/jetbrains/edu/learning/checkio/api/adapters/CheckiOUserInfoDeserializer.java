package com.jetbrains.edu.learning.checkio.api.adapters;

import com.google.gson.*;
import com.jetbrains.edu.learning.checkio.account.CheckiOUserInfo;

import java.lang.reflect.Type;

public class CheckiOUserInfoDeserializer implements JsonDeserializer<CheckiOUserInfo> {
  @Override
  public CheckiOUserInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    final JsonObject jsonObject = json.getAsJsonObject();

    final String username = jsonObject.get("username").getAsString();
    final int id = jsonObject.get("uid").getAsInt();

    return new CheckiOUserInfo(username, id);
  }
}
