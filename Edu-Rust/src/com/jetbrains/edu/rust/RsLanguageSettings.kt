package com.jetbrains.edu.rust

import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.rust.RsEduSettings

class RsLanguageSettings : LanguageSettings<RsEduSettings>() {
    override fun getSettings(): RsEduSettings = RsEduSettings
}
