package com.jetbrains.edu.go

import com.goide.sdk.combobox.GoSdkChooserCombo

fun validateSelectedSdk(sdkChooser: GoSdkChooserCombo): String? = sdkChooser.validator.validate(sdkChooser.sdk).errorMessage