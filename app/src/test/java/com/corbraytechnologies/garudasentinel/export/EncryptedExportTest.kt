package com.corbraytechnologies.garudasentinel.export

import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.AesVersion
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.util.Zip4jUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class EncryptedExportTest {

    private val content = """{"schemaVersion":2,"hello":"world"}""".toByteArray()
    private val password = "correct horse battery".toCharArray()

    private fun zipOf(password: CharArray = this.password): ByteArray {
        val out = ByteArrayOutputStream()
        EncryptedExport.write(out, content, password)
        return out.toByteArray()
    }

    @Test
    fun `the archive round trips with the right password`() {
        val zip = zipOf()
        ZipInputStream(ByteArrayInputStream(zip), password).use { stream ->
            val header = stream.nextEntry
            assertNotNull(header)
            assertEquals(EncryptedExport.ENTRY_NAME, header.fileName)
            assertTrue(header.isEncrypted)
            assertEquals(EncryptionMethod.AES, header.encryptionMethod)
            assertEquals(AesKeyStrength.KEY_STRENGTH_256, header.aesExtraDataRecord.aesKeyStrength)
            assertEquals(AesVersion.TWO, header.aesExtraDataRecord.aesVersion)
            assertEquals(String(content), String(stream.readBytes()))
            assertNull(stream.nextEntry)
        }
    }

    @Test
    fun `the content is not readable in the raw bytes`() {
        assertTrue(String(zipOf(), Charsets.ISO_8859_1).contains(EncryptedExport.ENTRY_NAME))
        assertTrue(!String(zipOf(), Charsets.ISO_8859_1).contains("schemaVersion"))
    }

    @Test
    fun `a wrong password fails`() {
        val zip = zipOf()
        assertThrows(Exception::class.java) {
            ZipInputStream(ByteArrayInputStream(zip), "wrong password!".toCharArray()).use { stream ->
                stream.nextEntry
                stream.readBytes()
            }
        }
    }

    @Test
    fun `short passwords are refused`() {
        assertThrows(IllegalArgumentException::class.java) { zipOf("short".toCharArray()) }
        assertEquals("Use at least 12 characters.", EncryptedExport.passwordProblem("short", "short"))
        assertEquals("The two passwords do not match.", EncryptedExport.passwordProblem("long enough pw", "other"))
        assertNull(EncryptedExport.passwordProblem("long enough pw", "long enough pw"))
    }

    @Test
    fun `strength follows the blocking and recommended lengths`() {
        assertEquals(EncryptedExport.PasswordStrength.TOO_SHORT, EncryptedExport.strength(0))
        assertEquals(EncryptedExport.PasswordStrength.TOO_SHORT, EncryptedExport.strength(11))
        assertEquals(EncryptedExport.PasswordStrength.SHORT, EncryptedExport.strength(12))
        assertEquals(EncryptedExport.PasswordStrength.SHORT, EncryptedExport.strength(19))
        assertEquals(EncryptedExport.PasswordStrength.GOOD, EncryptedExport.strength(20))
        assertEquals(EncryptedExport.PasswordStrength.GOOD, EncryptedExport.strength(64))
    }

    @Test
    fun `twelve characters are allowed with a hint, eleven are blocked`() {
        val eleven = "a".repeat(11)
        val twelve = "a".repeat(12)
        val twenty = "a".repeat(20)

        assertEquals("Use at least 12 characters.", EncryptedExport.passwordProblem(eleven, eleven))
        assertThrows(IllegalArgumentException::class.java) { zipOf(eleven.toCharArray()) }

        assertNull(EncryptedExport.passwordProblem(twelve, twelve))
        assertEquals(EncryptedExport.SHORT_PASSWORD_HINT, EncryptedExport.passwordHint(twelve))
        assertNotNull(zipOf(twelve.toCharArray()))

        assertNull(EncryptedExport.passwordProblem(twenty, twenty))
        assertNull(EncryptedExport.passwordHint(twenty))
    }

    @Test
    fun `no hint below the minimum, where the blocking message already speaks`() {
        assertNull(EncryptedExport.passwordHint(""))
        assertNull(EncryptedExport.passwordHint("a".repeat(11)))
    }

    @Test
    fun `the entry carries no date from this export`() {
        val zip = zipOf()
        ZipInputStream(ByteArrayInputStream(zip), password).use { stream ->
            val header = stream.nextEntry
            // A fixed date, so the archive says nothing about when it was made.
            // The header keeps DOS time, which has two-second resolution.
            val entryTime = Zip4jUtil.dosToExtendedEpochTme(header.lastModifiedTime)
            assertTrue(Math.abs(entryTime - EncryptedExport.FIXED_ENTRY_TIME) < 2_000)
            assertTrue(Math.abs(entryTime - System.currentTimeMillis()) > 365L * 24 * 3600 * 1000)
        }
    }
}
