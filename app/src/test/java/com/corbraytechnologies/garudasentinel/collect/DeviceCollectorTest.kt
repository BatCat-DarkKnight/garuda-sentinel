package com.corbraytechnologies.garudasentinel.collect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceCollectorTest {

    @Test
    fun `a charge cycle count of 0 or less is not reported`() {
        assertNull(DeviceCollector.cycleCountOrNull(0))
        assertNull(DeviceCollector.cycleCountOrNull(-1))
        assertEquals(312, DeviceCollector.cycleCountOrNull(312))
    }
}
