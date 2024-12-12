package com.jetbrains.edu.learning.marketplace

import com.intellij.ui.JBAccountInfoService
import com.intellij.ui.JBAccountInfoService.JBAData
import com.jetbrains.edu.learning.courseFormat.JBAccountUserInfo
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import io.mockk.every
import io.mockk.mockkStatic
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

fun loginFakeMarketplaceUser() {
  val account = MarketplaceAccount()
  account.userInfo = JBAccountUserInfo("Test User")
  MarketplaceSettings.INSTANCE.setAccount(account)
}

fun mockJBAccount() {
  mockkStatic(JBAccountInfoService::class)
  every { JBAccountInfoService.getInstance()?.userData } returns JBAData("test_id", "test user name", "test@email.com", null)
  every { JBAccountInfoService.getInstance()?.idToken } returns "eyJhbGciOiJSUzI1NiIsImtpZCI6InB1YmxpYzo5Mjg4ZTgyNC0yNGVhLTRhMGYtYWZkZC1kNzVlMzJhOTg5NzcifQ.eyJhdF9oYXNoIjoiNXMyWGtDczNPT05RODJHaXRKbF9OZyIsImF1ZCI6WyJpZGUiXSwiYXV0aF90aW1lIjoxNjc4NzA1MTMxLCJlbWFpbCI6InppbmFpZGEuc21pcm5vdmFAamV0YnJhaW5zLmNvbSIsImV4cCI6MTY3ODcwODczMiwiZnVsbF9uYW1lIjoiWmluYWlkYSBTbWlybm92YSIsImlhdCI6MTY3ODcwNTEzMiwiaXNzIjoiaHR0cHM6Ly9vYXV0aC5hY2NvdW50LmpldGJyYWlucy5jb20vIiwiamJhX2xvZ2luIjoiemluYWlkYXNtaXJub3ZhIiwianRpIjoiNDRjMjJlMWItOTIxNy00ZDNkLTg1MWEtNmI3MjIzNGIwNDJlIiwibGlua2VkX2VtYWlscyI6W10sInJhdCI6MTY3ODcwNTEwNywic2lkIjoiMWJjNmE4ZmUtNmFiNC00OTM2LWJlYWMtZTcyMTJlOWQ4NDc1Iiwic3ViIjoiNDQ5NDAyNiIsInVzZXJfaWQiOiJlanFkdTJtbnRpMDd2aXZwMTdjenNuMHRpIn0.MKGVh52BUIRpfO8lnn88mkPnw85xze10nghsQp4cNg2m-CFY4DB_CwLQlZKooUwK5G1KpXpOmkmtD4duZ2vdWArDR-hy3E1uB78p4FJ3RfS0qFiXZsS6AvmngMmL3y_nlknDArBY-bb_qIg4Hw8WIpvbC5H6wiwePtdWp-KJu6s_Xv1xCoZZEnEDQajFSBlX8ObeD_EbtA8xu_HkC1_m7vhBdSa9Nx9n5PYsXYltbNMT1DsRuWhqj3XlE6n4f5DauBWZTlwwH_-_4_3vI6FzEfUo-0G3mU2N0FsoooRBToRIVSvUMSR4Eh0tkDQBkQc0eCyl4g5pJFJST56do5MlBuO0yI074jp6SJm5c5IXWSUifWO3OcIvNVcDT4Ha0Q1JeaNAMEzIuLnxu9xmApZ16d74vPUMSJplribD3B0R_VToE_9I2kNrrYMjFPaaY_qk4S-3K53KDLKE2UxELI_II0pDBMK9j07lm_ss0h8RrUsSIOI5f3HVfljcFCo53XzttQYt8QGjpGpxYd5t4BuJFidlJPLBvjyphiIs__tX3ekjwucUP2EwjfNpO9NGQ25x9B_7P7aX2VBLU4jrFRjk4W-7Xr04rlnUQYpfB8JbTMWb5T7BKgMEi2asVwT1MMC6OHhS-g9TUImybq3AqZxLbV2xp3RBqvKKzuK4UgmyhIc"
  every { JBAccountInfoService.getInstance()?.accessToken } returns object : Future<String?> {
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
      return false
    }

    override fun isCancelled(): Boolean {
      return false
    }

    override fun isDone(): Boolean {
      return true
    }

    override fun get(): String? {
      return null
    }

    override fun get(timeout: Long, unit: TimeUnit): String {
      return "test token"
    }
  }

}