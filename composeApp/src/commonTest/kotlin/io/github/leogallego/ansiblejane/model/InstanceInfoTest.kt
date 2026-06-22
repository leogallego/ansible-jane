package io.github.leogallego.ansiblejane.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InstanceInfoTest {

    @Test
    fun `hasComponent returns true when component is present`() {
        val info = InstanceInfo(components = listOf("CONTROLLER", "GATEWAY", "EDA"))
        assertTrue(info.hasComponent(AapComponent.CONTROLLER))
        assertTrue(info.hasComponent(AapComponent.GATEWAY))
        assertTrue(info.hasComponent(AapComponent.EDA))
        assertFalse(info.hasComponent(AapComponent.HUB))
    }

    @Test
    fun `hasComponent returns false for empty components`() {
        val info = InstanceInfo()
        assertFalse(info.hasComponent(AapComponent.CONTROLLER))
    }

    @Test
    fun `resolvedPlatformType parses valid type`() {
        assertEquals(PlatformType.AAP, InstanceInfo(platformType = "AAP").resolvedPlatformType)
        assertEquals(PlatformType.AWX, InstanceInfo(platformType = "AWX").resolvedPlatformType)
        assertEquals(PlatformType.JEWEL, InstanceInfo(platformType = "JEWEL").resolvedPlatformType)
    }

    @Test
    fun `resolvedPlatformType returns UNKNOWN for invalid type`() {
        assertEquals(PlatformType.UNKNOWN, InstanceInfo(platformType = "BOGUS").resolvedPlatformType)
        assertEquals(PlatformType.UNKNOWN, InstanceInfo(platformType = "").resolvedPlatformType)
    }

    @Test
    fun `resolvedComponents parses valid components ignoring invalid ones`() {
        val info = InstanceInfo(components = listOf("CONTROLLER", "INVALID", "EDA"))
        val resolved = info.resolvedComponents
        assertEquals(setOf(AapComponent.CONTROLLER, AapComponent.EDA), resolved)
    }

    @Test
    fun `default InstanceInfo has empty values`() {
        val info = InstanceInfo()
        assertEquals("", info.controllerVersion)
        assertEquals("", info.gatewayVersion)
        assertEquals("", info.edaVersion)
        assertEquals("UNKNOWN", info.platformType)
        assertNull(info.aapVersion)
        assertTrue(info.components.isEmpty())
    }
}
