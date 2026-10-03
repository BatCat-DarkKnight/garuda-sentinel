/*
 * Garuda Sentinel, a personal Android privacy tool.
 * Copyright (C) 2025-2026 Karl Corbray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.corbraytechnologies.garudasentinel.export

import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.AesVersion
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.OutputStream
import java.util.Calendar
import java.util.TimeZone

/**
 * Password-protected export: a standard ZIP with AES-256 (WinZip AE-2) through zip4j, so it
 * opens in 7-Zip, Keka or The Unarchiver. No cryptography is implemented here.
 *
 * ZIP encryption hides the content but not the entry name or its timestamp, so the entry has
 * a fixed generic name and a fixed date: nothing about the user, device or export time.
 */
object EncryptedExport {
    const val ENTRY_NAME = "garuda-export.json"
    /** Below this the export is blocked. */
    const val MIN_PASSWORD_LENGTH = 12

    /** From this length on, no hint is shown. Shorter passwords are allowed, with a hint. */
    const val RECOMMENDED_PASSWORD_LENGTH = 20

    const val SHORT_PASSWORD_HINT = "Short. A few random words are stronger than one long word."

    /** 2000-01-01 00:00 UTC, used for the entry's visible timestamp. */
    val FIXED_ENTRY_TIME: Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(2000, Calendar.JANUARY, 1)
    }.timeInMillis

    enum class PasswordStrength { TOO_SHORT, SHORT, GOOD }

    /** Judged by length only: it is the one thing that reliably matters, and it is easy to explain. */
    fun strength(length: Int): PasswordStrength = when {
        length < MIN_PASSWORD_LENGTH -> PasswordStrength.TOO_SHORT
        length < RECOMMENDED_PASSWORD_LENGTH -> PasswordStrength.SHORT
        else -> PasswordStrength.GOOD
    }

    fun isPasswordAcceptable(password: CharArray): Boolean = strength(password.size) != PasswordStrength.TOO_SHORT

    /** Why the password cannot be used yet, or null when it is fine. */
    fun passwordProblem(password: String, repeat: String): String? = when {
        strength(password.length) == PasswordStrength.TOO_SHORT -> "Use at least $MIN_PASSWORD_LENGTH characters."
        password != repeat -> "The two passwords do not match."
        else -> null
    }

    /** Advice shown under the password field. It never blocks the export; [passwordProblem] does that. */
    fun passwordHint(password: String): String? =
        if (strength(password.length) == PasswordStrength.SHORT) SHORT_PASSWORD_HINT else null

    /** Writes [content] as the single encrypted entry of a ZIP to [out], then closes [out]. */
    fun write(out: OutputStream, content: ByteArray, password: CharArray) {
        require(isPasswordAcceptable(password)) { "The password must have at least $MIN_PASSWORD_LENGTH characters." }
        val parameters = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            isEncryptFiles = true
            encryptionMethod = EncryptionMethod.AES
            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            aesVersion = AesVersion.TWO
            fileNameInZip = ENTRY_NAME
            lastModifiedFileTime = FIXED_ENTRY_TIME
        }
        ZipOutputStream(out, password).use { zip ->
            zip.putNextEntry(parameters)
            zip.write(content)
            zip.closeEntry()
        }
    }
}
