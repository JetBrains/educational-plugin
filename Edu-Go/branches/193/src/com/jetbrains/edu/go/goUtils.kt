package com.jetbrains.edu.go

import com.goide.sdk.combobox.GoSdkChooserCombo

fun getSdkValidationMessage(sdkChooser: GoSdkChooserCombo): String? = sdkChooser.validator.validate(sdkChooser.sdk).errorMessage