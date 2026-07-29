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

# Google API Client / Drive (Stage C) - these libraries parse their model
# classes via reflection (fields annotated with @Key), so keep field names.
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keepclassmembers class * extends com.google.api.client.json.GenericJson { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.api.services.**
-dontwarn org.apache.http.**
-dontwarn org.apache.commons.**
-dontwarn org.joda.time.**
