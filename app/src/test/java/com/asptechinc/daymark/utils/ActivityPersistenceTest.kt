package com.asptechinc.daymark.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityPersistenceTest {
    @Test
    fun parseCategoryId_handlesLegacyNullValues() {
        assertNull(ActivityPersistence.parseCategoryId("null"))
        assertNull(ActivityPersistence.parseCategoryId(""))
        assertNull(ActivityPersistence.parseCategoryId("   "))
        assertEquals(7, ActivityPersistence.parseCategoryId("7"))
    }

    @Test
    fun parseTagIds_handlesLegacyNullValues() {
        assertTrue(ActivityPersistence.parseTagIds("null").isEmpty())
        assertTrue(ActivityPersistence.parseTagIds("").isEmpty())
        assertTrue(ActivityPersistence.parseTagIds("  ").isEmpty())

        val tags = ActivityPersistence.parseTagIds("1, 2, 3")
        assertEquals(3, tags.size)
        assertEquals(1, tags[0])
        assertEquals(2, tags[1])
        assertEquals(3, tags[2])
    }

    @Test
    fun parseTagIds_handlesMalformedStrings() {
        val tags = ActivityPersistence.parseTagIds("1,abc,null, 4")
        assertEquals(2, tags.size)
        assertEquals(1, tags[0])
        assertEquals(4, tags[1])
    }
}
