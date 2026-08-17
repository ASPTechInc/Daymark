package com.asptechinc.daymark.utils

import android.content.Context
import com.asptechinc.daymark.models.ChangelogSection
import com.asptechinc.daymark.models.ChangelogVersion
import java.io.BufferedReader
import java.io.InputStreamReader

object ChangelogParser {
    fun parse(context: Context): List<ChangelogVersion> {
        val versions = mutableListOf<ChangelogVersion>()
        try {
            val inputStream = context.assets.open("CHANGELOG.md")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = reader.readLine()

            var currentVersion: String? = null
            var currentDate: String? = null
            var currentDescription = StringBuilder()
            var currentSections = mutableListOf<ChangelogSection>()
            var currentSectionTitle: String? = null
            var currentSectionItems = mutableListOf<String>()

            while (line != null) {
                val trimmedLine = line.trim()

                when {
                    trimmedLine.startsWith("# ") -> {
                        // Save previous version if exists
                        if (currentVersion != null) {
                            if (currentSectionTitle != null) {
                                currentSections.add(
                                    ChangelogSection(
                                        currentSectionTitle,
                                        currentSectionItems.toList(),
                                    ),
                                )
                            }
                            versions.add(
                                ChangelogVersion(
                                    currentVersion,
                                    currentDate,
                                    currentDescription.toString().trim().takeIf { it.isNotEmpty() },
                                    currentSections.toList(),
                                ),
                            )
                        }

                        // Start new version
                        val header = trimmedLine.substring(2)
                        // Regex to match [v1.0](url) (date) or v1.0 (date)
                        val versionMatch =
                            Regex("""(?:\[([^\]]+)\](?:\([^\)]+\))?|([^\s(]+))\s*(?:\(([^)]+)\))?""").find(
                                header,
                            )

                        currentVersion = versionMatch
                            ?.let {
                                it.groupValues[1].takeIf { g -> g.isNotEmpty() }
                                    ?: it.groupValues[2].takeIf { g -> g.isNotEmpty() }
                            }?.trim() ?: header

                        currentDate = versionMatch?.groupValues?.get(3)?.trim()
                        currentDescription = StringBuilder()
                        currentSections = mutableListOf()
                        currentSectionTitle = null
                        currentSectionItems = mutableListOf()
                    }

                    trimmedLine.startsWith("### ") -> {
                        // Save previous section
                        if (currentSectionTitle != null) {
                            currentSections.add(
                                ChangelogSection(
                                    currentSectionTitle,
                                    currentSectionItems.toList(),
                                ),
                            )
                        }
                        currentSectionTitle = trimmedLine.substring(4)
                        currentSectionItems = mutableListOf()
                    }

                    trimmedLine.startsWith("* ") || trimmedLine.startsWith("- ") -> {
                        val item = trimmedLine.substring(2)
                        currentSectionItems.add(item)
                    }

                    trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#") -> {
                        if (currentSectionTitle == null) {
                            currentDescription.append(line).append("\n")
                        } else {
                            // Multi-line items? For now just append to last item if it exists
                            if (currentSectionItems.isNotEmpty()) {
                                val lastItem =
                                    currentSectionItems.removeAt(currentSectionItems.size - 1)
                                currentSectionItems.add("$lastItem $trimmedLine")
                            }
                        }
                    }
                }
                line = reader.readLine()
            }

            // Add last version
            if (currentVersion != null) {
                if (currentSectionTitle != null) {
                    currentSections.add(
                        ChangelogSection(
                            currentSectionTitle,
                            currentSectionItems.toList(),
                        ),
                    )
                }
                versions.add(
                    ChangelogVersion(
                        currentVersion,
                        currentDate,
                        currentDescription.toString().trim().takeIf { it.isNotEmpty() },
                        currentSections.toList(),
                    ),
                )
            }

            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return versions
    }
}
