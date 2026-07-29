# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# SQLCipher / Conscrypt optional crypto providers referenced but not present at runtime
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
