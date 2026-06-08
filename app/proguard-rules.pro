# ============================================================================
# kidZone ProGuard / R8 rules
# ============================================================================

# --- General ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable  # readable crash stack traces
-renamesourcefileattribute SourceFile

# --- Firebase ---
# Firebase Firestore DTO classes – need no-arg constructor + field names
-keep class com.kidzone.data.remote.dto.** { *; }
# Firebase Auth internal
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- Hilt / Dagger ---
-dontwarn dagger.**
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# --- Gson (used by Retrofit converter) ---
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Retrofit ---
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Coil ---
-dontwarn coil.**

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * implements androidx.room.RoomDatabase$Callback { *; }
-dontwarn androidx.room.**

# --- Compose ---
# Compose uses reflection for state management; R8 handles most of it
# automatically with AGP 8+, but we keep stability annotations.
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- Google Maps ---
-keep class com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.**

# --- Google Play Services Auth / Credential Manager ---
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.libraries.identity.** { *; }
-keep class androidx.credentials.** { *; }

# --- Kotlin Coroutines ---
-dontwarn kotlinx.coroutines.**

# --- Domain models (parcelize / serialization future-proofing) ---
-keep class com.kidzone.domain.model.** { *; }

# --- Enums ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
