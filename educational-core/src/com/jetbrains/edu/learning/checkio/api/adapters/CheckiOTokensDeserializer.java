package com.jetbrains.edu.learning.checkio.api.adapters;

import com.google.gson.*;
import com.jetbrains.edu.learning.authUtils.TokenInfo;

import java.lang.reflect.Type;

public class CheckiOTokensDeserializer implements JsonDeserializer<TokenInfo> {
  @Override
  public TokenInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
    final JsonObject jsonObject = json.getAsJsonObject();

    final String accessToken = jsonObject.get("access_token").getAsString();
    final String refreshToken = jsonObject.get("refresh_token").getAsString();
    final long expiresIn = jsonObject.get("expires_in").getAsLong();

    final long expiringTime = expiresIn + (System.currentTimeMillis() / 1000);

    TokenInfo tokenInfo = new TokenInfo();
    tokenInfo.setRefreshToken(refreshToken);
    tokenInfo.setAccessToken(accessToken);
    tokenInfo.setExpiresIn(expiringTime);
    return tokenInfo;
  }
}
