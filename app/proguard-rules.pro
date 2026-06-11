# ============================================================================
# kidZone ProGuard / R8 rules
# ============================================================================

# --- General ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable  # readable crash stack traces
-renamesourcefileattribute SourceFile

# --- Firebase ---
# Firestore DTO classes need no-arg constructor + field names for reflection
-keep class com.kidzone.data.remote.dto.** { *; }
# Firebase App Check provider reflection
-keep class com.google.firebase.appcheck.** { *; }
-dontwarn com.google.firebase.**

# --- Hilt / Dagger ---
-dontwarn dagger.**
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# --- Gson (used by Retrofit converter) ---
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Retrofit ---
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# --- Google Maps ---
-keep class com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.maps.**

# --- Google Play Services Auth / Credential Manager ---
-keep class com.google.android.gms.auth.api.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }
-dontwarn com.google.android.libraries.identity.**

# --- Kotlin Coroutines ---
-dontwarn kotlinx.coroutines.**

# --- Domain models (serialization / Parcelable future-proofing) ---
-keep class com.kidzone.domain.model.** { *; }

# --- Enums ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
