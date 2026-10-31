package com.dude555afk.xeno.core.fetch

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerEnginePlanTest {

    @Test
    fun keepsAtMostTwoDistinctFocusedQueries() {
        val queries = AnswerEngine.parseFollowUpQueries(
            "1. recent independent verification\n- implementation details\n3. extra angle\n",
            "Original question?"
        )

        assertEquals(
            listOf("recent independent verification", "implementation details"),
            queries
        )
    }

    @Test
    fun ignoresNoneAndTheOriginalQuestion() {
        assertEquals(
            emptyList<String>(),
            AnswerEngine.parseFollowUpQueries("NONE", "Original question?")
        )
        assertEquals(
            emptyList<String>(),
            AnswerEngine.parseFollowUpQueries("Original question?", "Original question?")
        )
    }
}
