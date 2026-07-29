# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# SQLCipher / Conscrypt optional crypto providers referenced but not present at runtime
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Tink (used by androidx.security:security-crypto) references compile-time-only
# JSR305 annotations that aren't present at runtime; safe to ignore.
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
