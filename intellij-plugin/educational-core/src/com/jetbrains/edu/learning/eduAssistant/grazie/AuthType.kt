package com.jetbrains.edu.learning.eduAssistant.grazie

import ai.grazie.model.auth.v5.AuthData
import ai.grazie.model.cloud.AuthType

enum class GrazieAuthType(val grazieType: AuthType) {
    User(AuthType.User),
    Service(AuthType.Service)
    ;

    fun buildAuthData(jwtToken: String): AuthData = when(this) {
        User -> AuthData(jwtToken)
        Service -> AuthData(jwtToken, originalServiceToken = jwtToken)
    }
}
