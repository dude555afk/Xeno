package com.dude555afk.xeno

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device/emulator smoke test. This catches startup crashes that unit tests
 * cannot see; it deliberately does not claim that live websites or AI work.
 */
@RunWith(AndroidJUnit4::class)
class BrowserStartupSmokeTest {

    @Test
    fun mainActivityStartsAndRemainsAlive() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse("MainActivity finished immediately after launch", activity.isFinishing)
            }
        }
    }
}
