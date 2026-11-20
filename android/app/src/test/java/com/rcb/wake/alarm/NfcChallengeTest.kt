package com.rcb.wake.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NfcChallengeTest {
    @Test fun `tag hash is stable, salted and never the raw id`() {
        val uid = byteArrayOf(0x04, 0x1A, 0x7F, 0x00)
        val a = NfcTags.hash(uid, "salt-1")
        assertEquals(a, NfcTags.hash(uid, "salt-1"))
        assertNotEquals(a, NfcTags.hash(uid, "salt-2"))
        assertNotEquals(a, NfcTags.hash(byteArrayOf(0x04, 0x1A, 0x7F, 0x01), "salt-1"))
        assertEquals(64, a.length)
        assertEquals(false, a.contains("041a7f00"))
    }

    @Test fun `any nfc alarm makes the session require a tag`() {
        val session = AlarmSession.start(sampleAlarm(id = 1, challengeEnabled = true), 0)
        assertEquals(ChallengeType.MATH, session.challengeType)
        val joined = session.withAlarm(sampleAlarm(id = 2, challengeEnabled = true, challengeType = ChallengeType.NFC))
        assertEquals(ChallengeType.NFC, joined.challengeType)
    }

    @Test fun `nfc type is ignored when the challenge is off`() {
        val session = AlarmSession.start(sampleAlarm(id = 1, challengeEnabled = false, challengeType = ChallengeType.NFC), 0)
        assertEquals(ChallengeType.MATH, session.challengeType)
    }

    @Test(expected = InvalidAlarmException::class)
    fun `unknown challenge type is rejected`() {
        AlarmValidator.validate(
            AlarmInput(null, 7, 0, true, 0, "", "builtin:dawn", "Dawn", true, true, 10, true, "easy", 3, challengeType = "qr"),
        )
    }
}
