package com.jetbrains.edu.learning.checkio.api.adapters;

import com.google.gson.*;
import com.jetbrains.edu.learning.checkio.account.CheckiOTokens;

import java.lang.reflect.Type;

public class CheckiOTokensDeserializer implements JsonDeserializer<CheckiOTokens> {
  @Override
  public CheckiOTokens deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    final JsonObject jsonObject = json.getAsJsonObject();

    final String accessToken = jsonObject.get("access_token").getAsString();
    final String refreshToken = jsonObject.get("refresh_token").getAsString();
    final long expiresIn = jsonObject.get("expires_in").getAsLong();

    final long expiringTime = expiresIn + (System.currentTimeMillis() / 1000);

    return new CheckiOTokens(accessToken, refreshToken, expiringTime);
  }
}
