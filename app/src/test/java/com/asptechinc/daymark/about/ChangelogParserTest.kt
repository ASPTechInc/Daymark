package com.asptechinc.daymark.about

import android.content.Context
import android.content.res.AssetManager
import com.asptechinc.daymark.utils.ChangelogParser
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.ByteArrayInputStream

class ChangelogParserTest {
    @Test
    fun testParse() {
        val mockAssets =
            mock<AssetManager> {
                on { open("CHANGELOG.md") } doReturn
                    ByteArrayInputStream(
                        """
                        # [v1.0](https://github.com/v1.0) (2026-08-16)
                        
                        Initial release.
                        
                        ### Features
                        * Feature 1
                        * Feature 2
                        
                        # v0.9 (2026-08-01)
                        ### Fixes
                        * Fix 1
                        """.trimIndent().toByteArray(),
                    )
            }
        val mockContext =
            mock<Context> {
                on { assets } doReturn mockAssets
            }

        val versions = ChangelogParser.parse(mockContext)

        assertEquals(2, versions.size)

        val v1 = versions[0]
        assertEquals("v1.0", v1.version)
        assertEquals("2026-08-16", v1.date)
        assertEquals("Initial release.", v1.description)
        assertEquals(1, v1.sections.size)
        assertEquals("Features", v1.sections[0].title)
        assertEquals(2, v1.sections[0].items.size)
        assertEquals("Feature 1", v1.sections[0].items[0])

        val v09 = versions[1]
        assertEquals("v0.9", v09.version)
        assertEquals("2026-08-01", v09.date)
        assertEquals(null, v09.description)
        assertEquals(1, v09.sections.size)
        assertEquals("Fixes", v09.sections[0].title)
        assertEquals(1, v09.sections[0].items.size)
    }
}
