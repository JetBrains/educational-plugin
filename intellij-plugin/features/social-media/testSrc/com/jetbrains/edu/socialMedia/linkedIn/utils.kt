package com.jetbrains.edu.socialMedia.linkedIn

@Suppress("UnusedReceiverParameter")
fun LinkedInAccount.Factory.create(): LinkedInAccount {
  val tokenExpiresIn = System.currentTimeMillis() + 10000
  return LinkedInAccount(LinkedInUserInfo("foo", "bar"), tokenExpiresIn)
}
