package com.jetbrains.edu.learning.codeforces.api


interface ContestRegistration

class ContestRegistrationError : ContestRegistration

class ContestRegistrationData(val token: String, val termsOfAgreement: String, val isTeamRegistrationAvailable: Boolean) : ContestRegistration