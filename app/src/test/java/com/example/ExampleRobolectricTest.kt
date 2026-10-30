package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SecurityManager
import com.example.data.VerifiedPasswordType
import com.example.model.WhatsAppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Device Audit Log", appName)
    }

    @Test
    fun `test dual password security setup and verification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = SecurityManager(context)

        // Setup passwords
        val setupResult = securityManager.setupPasswords("Admin@1234", "Backup#5678", "School name")
        assertTrue(setupResult)
        assertTrue(securityManager.isSetupCompleted())

        // Verify with Master password
        val masterVerified = securityManager.verifyPassword("Admin@1234")
        assertEquals(VerifiedPasswordType.MASTER_ADMIN, masterVerified)

        // Verify with Backup password
        val backupVerified = securityManager.verifyPassword("Backup#5678")
        assertEquals(VerifiedPasswordType.SECONDARY_BACKUP, backupVerified)

        // Verify wrong password fails
        val wrongVerified = securityManager.verifyPassword("WrongPassword")
        assertNull(wrongVerified)
    }

    @Test
    fun `test lending session start and end tracking`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = SecurityManager(context)

        val session = securityManager.startSession("Friend Bilal")
        assertNotNull(session)
        assertTrue(securityManager.isSessionActive())
        assertEquals("Friend Bilal", session.borrowerName)

        val endedSession = securityManager.endSession(totalCalls = 3, totalSms = 2, totalWhatsApp = 1)
        assertNotNull(endedSession)
        assertFalse(securityManager.isSessionActive())
        assertEquals(3, endedSession?.callsRecordedCount)
        assertEquals(2, endedSession?.smsRecordedCount)
        assertEquals(1, endedSession?.whatsappRecordedCount)
    }

    @Test
    fun `test custom app name and icon disguise`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = SecurityManager(context)

        securityManager.setCustomAppName("Calculator Plus")
        securityManager.setCustomAppIcon("CALCULATOR")

        assertEquals("Calculator Plus", securityManager.getCustomAppName())
        assertEquals("CALCULATOR", securityManager.getCustomAppIcon())
    }

    @Test
    fun `test whatsapp notification storage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = SecurityManager(context)

        val now = System.currentTimeMillis()
        val entry = WhatsAppEntry(
            id = 555L,
            senderOrCaller = "Ali WhatsApp",
            body = "Meeting at 4pm",
            isCall = false,
            timestamp = now
        )

        securityManager.saveWhatsAppEntry(entry)
        val logs = securityManager.getWhatsAppLogs()
        assertTrue(logs.any { it.senderOrCaller == "Ali WhatsApp" && it.body == "Meeting at 4pm" })
    }
}
