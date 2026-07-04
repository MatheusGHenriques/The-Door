package io.github.matheusghenriques.thedoor.data

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLimitRepositoryJsonTest {

    // region configsToJson / parseConfigs

    @Test
    fun configsToJson_parseConfigs_roundTrip() {
        val configs = mapOf(
            "com.example.app1" to AppConfig(
                blocked = true,
                perDayLimits = mapOf(Calendar.MONDAY to 30),
                perDayTargets = mapOf(Calendar.MONDAY to 15),
                lastReductionWeekStart = 1000L
            ),
            "com.example.app2" to AppConfig()
        )
        val json = configsToJson(configs)
        val parsed = parseConfigs(json)
        assertEquals(configs, parsed)
    }

    @Test
    fun parseConfigs_emptyJson_returnsEmptyMap() {
        val result = parseConfigs("{}")
        assertTrue(result.isEmpty())
    }

    @Test
    fun parseConfigs_missingOptionalFields_returnsDefaults() {
        val json = """{"com.example.app":{"blocked":true,"lastReduction":0}}"""
        val result = parseConfigs(json)
        assertEquals(true, result["com.example.app"]?.blocked)
        assertTrue(result["com.example.app"]?.perDayLimits?.isEmpty() == true)
        assertTrue(result["com.example.app"]?.perDayTargets?.isEmpty() == true)
    }

    @Test
    fun configsToJson_emptyMap_returnsEmptyObject() {
        assertEquals("{}", configsToJson(emptyMap()))
    }

    @Test
    fun parseConfigs_partialConfig_parsesCorrectly() {
        val json = """{"app":{"blocked":true,"perDay":{"2":45},"lastReduction":5000}}"""
        val result = parseConfigs(json)
        val config = result["app"]
        assertEquals(true, config?.blocked)
        assertEquals(mapOf(Calendar.MONDAY to 45), config?.perDayLimits)
        assertTrue(config?.perDayTargets?.isEmpty() == true)
        assertEquals(5000L, config?.lastReductionWeekStart)
    }

    // endregion

    // region redirectConfigToJson / parseRedirectConfig

    @Test
    fun redirectConfigToJson_parseRedirectConfig_roundTrip() {
        val config = RedirectConfig(
            type = RedirectType.APP,
            appPackage = "com.example.target",
            theDoorPhrase = "focus",
            toastEnabled = true,
            toastMessage = "Stay focused"
        )
        val json = redirectConfigToJson(config)
        val parsed = parseRedirectConfig(json)
        assertEquals(config, parsed)
    }

    @Test
    fun parseRedirectConfig_emptyJson_returnsDefaults() {
        val result = parseRedirectConfig("{}")
        assertEquals(RedirectType.THE_DOOR, result.type)
        assertEquals("", result.appPackage)
        assertEquals("", result.theDoorPhrase)
        assertEquals(false, result.toastEnabled)
        assertEquals("", result.toastMessage)
    }

    @Test
    fun parseRedirectConfig_invalidType_usesDefault() {
        val json = """{"type":"INVALID_TYPE","appPackage":"pkg","theDoorPhrase":"p","toastEnabled":true,"toastMessage":"m"}"""
        val result = parseRedirectConfig(json)
        assertEquals(RedirectType.THE_DOOR, result.type)
    }

    // endregion
}
