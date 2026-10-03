package com.corbraytechnologies.garudasentinel.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteSummaryTest {

    @Test
    fun `names the counts and the chosen folder`() {
        assertEquals(
            "Removes 3 saved checks, 2 location readings and access to your Download folder. " +
                "Files you exported stay where you saved them.",
            DeleteSummary.sentence(3, 2, "Download"),
        )
    }

    @Test
    fun `singular counts and no folder`() {
        assertEquals(
            "Removes 1 saved check and 1 location reading. Files you exported stay where you saved them.",
            DeleteSummary.sentence(1, 1, null),
        )
    }

    @Test
    fun `the storage root has no folder name`() {
        assertEquals(
            "Removes 0 saved checks, 0 location readings and access to the folder you chose. " +
                "Files you exported stay where you saved them.",
            DeleteSummary.sentence(0, 0, ""),
        )
    }
}
