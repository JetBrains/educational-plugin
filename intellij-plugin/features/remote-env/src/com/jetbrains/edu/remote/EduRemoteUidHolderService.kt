package com.jetbrains.edu.remote

import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class EduRemoteUidHolderService {
  var userUid: String? = null
    internal set
}