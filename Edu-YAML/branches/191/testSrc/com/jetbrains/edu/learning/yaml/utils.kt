package com.jetbrains.edu.learning.yaml

import com.jetbrains.edu.learning.EduUtils

// tests fail in AS 191 because of Feature Usage Statistics exception
val skipYamlCompletionTests: Boolean by lazy {
  EduUtils.isAndroidStudio()
}
