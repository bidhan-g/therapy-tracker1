package com.bidh.therapytracker.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

object SecurePrefs {

    private const val PREFS_NAME = "therapy_tracker_secure_prefs"
    private const val KEY_TARGET_SESSIONS = "target_sessions"
    private const val KEY_LOCK_ENABLED = "lock_enabled"
    private const val KEY_MORNING_HOUR = "morning_hour"
    private const val KEY_MORNING_MINUTE = "morning_minute"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"

    private fun prefs(context: Context) = run {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // -1 means the user hasn't chosen a total yet.
    fun getTargetSessions(context: Context): Int =
        prefs(context).getInt(KEY_TARGET_SESSIONS, -1)

    fun isTargetSet(context: Context): Boolean =
        getTargetSessions(context) > 0

    fun setTargetSessions(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_TARGET_SESSIONS, value).apply()
    }

    fun clearTargetSessions(context: Context) {
        prefs(context).edit().remove(KEY_TARGET_SESSIONS).apply()
    }

    fun isLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOCK_ENABLED, false)

    fun setLockEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOCK_ENABLED, value).apply()
    }

    fun getMorningHour(context: Context): Int =
        prefs(context).getInt(KEY_MORNING_HOUR, 8)

    fun getMorningMinute(context: Context): Int =
        prefs(context).getInt(KEY_MORNING_MINUTE, 0)

    fun setMorningTime(context: Context, hour: Int, minute: Int) {
        prefs(context).edit()
            .putInt(KEY_MORNING_HOUR, hour)
            .putInt(KEY_MORNING_MINUTE, minute)
            .apply()
    }

    fun getOrCreateDbPassphrase(context: Context): ByteArray {
        val p = prefs(context)
        val existing = p.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }
        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        p.edit().putString(KEY_DB_PASSPHRASE, Base64.encodeToString(random, Base64.NO_WRAP)).apply()
        return random
    }
}
