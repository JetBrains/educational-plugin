package com.jetbrains.edu.learning.marketplace.certificate

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test

class CourseCertificateBannerManagerTest : EduSettingsServiceTestBase() {

  @Test
  fun `test empty state serialization`() {
    val manager = CourseCertificateBannerManager()
    manager.loadStateAndCheck("""
      <state><![CDATA[{}]]></state>
    """)
    assertNull(manager.certificateId.value)
  }

  @Test
  fun `test certificate id serialization`() {
    val manager = CourseCertificateBannerManager()
    manager.loadStateAndCheck("""
      <state><![CDATA[{
        "certificateId": "cert-ABCD"
      }]]></state>
    """)
    assertEquals("cert-ABCD", manager.certificateId.value)
  }

  @Test
  fun `test state is serialized after show`() {
    val manager = CourseCertificateBannerManager()
    manager.show("certificate-id")
    assertEquals("certificate-id", manager.certificateId.value)
    manager.checkState("""
      <state><![CDATA[{
        "certificateId": "certificate-id"
      }]]></state>
    """)
  }

  @Test
  fun `test state is serialized after dismiss`() {
    val manager = CourseCertificateBannerManager()
    manager.show("certificate-id")
    manager.dismiss()
    assertNull(manager.certificateId.value)
    manager.checkState("""
      <state><![CDATA[{}]]></state>
    """)
  }
}
