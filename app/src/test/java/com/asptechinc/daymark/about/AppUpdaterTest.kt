package com.asptechinc.daymark.about

import com.asptechinc.daymark.utils.AppUpdater
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterTest {
    @Test
    fun testVersionComparison() {
        assertTrue(AppUpdater.isNewerVersion("1.0", "1.1"))
        assertTrue(AppUpdater.isNewerVersion("1.0.0", "1.0.1"))
        assertTrue(AppUpdater.isNewerVersion("v1.0", "v1.1"))
        assertTrue(AppUpdater.isNewerVersion("1.1", "2.0"))
        assertTrue(AppUpdater.isNewerVersion("1.0.9", "1.1.0"))

        assertFalse(AppUpdater.isNewerVersion("1.1", "1.0"))
        assertFalse(AppUpdater.isNewerVersion("1.1", "1.1"))
        assertFalse(AppUpdater.isNewerVersion("1.1.0", "1.1"))
        assertFalse(AppUpdater.isNewerVersion("2.0", "1.9.9"))
    }
}
