package com.rcb.wake.alarm

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import com.rcb.wake.storage.AppStorage
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

data class NfcTag(val id: String, val name: String, val hash: String)

/**
 * NFC tags registered for the "scan a tag to dismiss" challenge.
 *
 * Only a salted SHA-256 of each tag's hardware id is stored, never the id itself. Matching is
 * done natively, so the challenge works on the lock screen without React Native.
 */
object NfcTags {
    private const val KEY_TAGS = "nfc_tags"
    private const val KEY_SALT = "nfc_salt"
    const val MAX_TAGS = 5

    /**
     * The NFC challenge is on hold and the NFC permission is not declared. Without it, even asking
     * whether NFC is on makes some systems (Xiaomi HyperOS) show an error, so every NFC call is
     * skipped unless the permission is present.
     */
    private fun permitted(context: Context) =
        context.checkSelfPermission(android.Manifest.permission.NFC) == PackageManager.PERMISSION_GRANTED

    fun isSupported(context: Context) =
        permitted(context) && context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)

    fun isEnabled(context: Context) = permitted(context) && NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    /** Pure hashing so tests can check it: hex(sha256(salt + ":" + hex(uid))). */
    fun hash(uid: ByteArray, salt: String): String {
        val input = "$salt:" + uid.joinToString("") { "%02x".format(it) }
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun salt(context: Context): String {
        val p = AppStorage.settings(context)
        return p.getString(KEY_SALT, null) ?: UUID.randomUUID().toString().also { p.edit().putString(KEY_SALT, it).apply() }
    }

    fun hashOf(context: Context, tag: Tag) = hash(tag.id, salt(context))

    fun all(context: Context): List<NfcTag> {
        val raw = AppStorage.settings(context).getString(KEY_TAGS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                NfcTag(o.getString("id"), o.getString("name"), o.getString("hash"))
            }
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, tags: List<NfcTag>) {
        val arr = JSONArray()
        tags.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("hash", it.hash)) }
        AppStorage.settings(context).edit().putString(KEY_TAGS, arr.toString()).apply()
    }

    /** Adds a tag; returns the existing entry if the same physical tag is already registered. */
    fun add(context: Context, name: String, hash: String): NfcTag {
        val tags = all(context)
        tags.firstOrNull { it.hash == hash }?.let { return it }
        require(tags.size < MAX_TAGS) { "At most $MAX_TAGS tags" }
        val tag = NfcTag(UUID.randomUUID().toString(), name.trim().ifBlank { "Tag ${tags.size + 1}" }.take(40), hash)
        save(context, tags + tag)
        return tag
    }

    fun remove(context: Context, id: String) = save(context, all(context).filterNot { it.id == id })

    fun isRegistered(context: Context, hash: String) = all(context).any { it.hash == hash }

    /** Reads tags in reader mode on [activity] until [disable] is called. Callback runs on a binder thread. */
    fun enableReader(activity: Activity, onTag: (Tag) -> Unit): Boolean {
        if (!permitted(activity)) return false
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return false
        if (!adapter.isEnabled) return false
        val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NFC_BARCODE or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        adapter.enableReaderMode(activity, { tag -> onTag(tag) }, flags, null)
        return true
    }

    fun disableReader(activity: Activity) {
        if (!permitted(activity)) return
        runCatching { NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity) }
    }
}
