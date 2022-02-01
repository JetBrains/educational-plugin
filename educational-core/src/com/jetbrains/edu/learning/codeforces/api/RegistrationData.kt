package com.jetbrains.edu.learning.codeforces.api


interface RegistrationData

class RegistrationFailed : RegistrationData

class RegistrationCompleted(val token: String, val termsOfAgreement: String, val isTeamRegistrationAvailable: Boolean) : RegistrationData