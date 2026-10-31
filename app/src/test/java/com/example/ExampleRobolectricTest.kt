package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SecurityManager
import com.example.model.WhatsAppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun `test single master pin setup and verification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = SecurityManager(context)

        // Setup PIN
        val setupResult = securityManager.setupPin("1234", "Favorite year")
        assertTrue(setupResult)
        assertTrue(securityManager.isSetupCompleted())

        // Verify with correct PIN
        val verified = securityManager.verifyPin("1234")
        assertTrue(verified)

        // Verify wrong PIN fails
        val wrongVerified = securityManager.verifyPin("9999")
        assertFalse(wrongVerified)

        // Verify update PIN
        val updateOk = securityManager.updatePin("1234", "5678", "New hint")
        assertTrue(updateOk)
        assertTrue(securityManager.verifyPin("5678"))
        assertFalse(securityManager.verifyPin("1234"))
    }

    @Test
    fun `test biometric settings toggle`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val securityManager = SecurityManager(context)

        securityManager.setBiometricEnabled(true)
        assertTrue(securityManager.isBiometricEnabled())

        securityManager.setBiometricEnabled(false)
        assertFalse(securityManager.isBiometricEnabled())
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
    fun `test whatsapp notification storage and clearing`() {
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

        // Test clear history
        securityManager.clearAllHistory()
        val clearedLogs = securityManager.getWhatsAppLogs()
        assertTrue(clearedLogs.isEmpty())
        assertFalse(securityManager.isSessionActive())
    }
}
