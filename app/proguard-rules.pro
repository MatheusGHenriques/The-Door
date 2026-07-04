# Argon2Kt - JNI native bindings
-keep class com.lambdapioneer.argon2kt.** { *; }
-keepnames class com.lambdapioneer.argon2kt.**

# EncryptedSharedPreferences (security-crypto)
-keep class androidx.security.crypto.** { *; }

# DataStore - uses reflection for Preferences serialization
-keep class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }

# JSON serialization in AppLimitRepository
-keep class org.json.** { *; }

# Tink (bundled with security-crypto) - missing optional annotations
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# R8 Kotlin metadata parsing warnings — harmless, expected with Kotlin 2.3.0
-ignorewarnings
