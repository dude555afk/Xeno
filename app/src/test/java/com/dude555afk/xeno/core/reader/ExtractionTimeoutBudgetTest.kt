package com.dude555afk.xeno.core.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ExtractionTimeoutBudgetTest {

    @Test
    fun pageLoadCannotConsumeTheWholeExtractionBudget() {
        assertEquals(15_000L, ExtractionTimeoutBudget.pageLoadTimeoutMs(25_000L))
        assertEquals(6_000L, ExtractionTimeoutBudget.pageLoadTimeoutMs(9_000L))
    }

    @Test
    fun shortOrInvalidBudgetsStillProduceAPositiveLoadTimeout() {
        assertEquals(1L, ExtractionTimeoutBudget.pageLoadTimeoutMs(1L))
        assertEquals(1L, ExtractionTimeoutBudget.pageLoadTimeoutMs(0L))
        assertEquals(1L, ExtractionTimeoutBudget.pageLoadTimeoutMs(-500L))
    }
}
