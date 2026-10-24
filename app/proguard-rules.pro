# R8 (release). Retrofit, OkHttp, Hilt, Compose & kotlinx-serialization membawa aturan sendiri.

# --- Gson: model dibaca lewat refleksi (nama field = key JSON) -------------------------------
# Konvensi: semua DTO/request/response ada di paket `feature.<fitur>.data` atau envelope di
# `core.network`. Model Gson di luar paket itu WAJIB ditambahkan di sini, atau field-nya
# di-rename R8 dan terbaca null di release.
-keep class com.mhyddnn.mubarokappmobile.core.network.ApiResponse { <fields>; <init>(...); }
-keep class com.mhyddnn.mubarokappmobile.core.network.Meta { <fields>; <init>(...); }
-keep class com.mhyddnn.mubarokappmobile.feature.**.data.** { <fields>; <init>(...); }

# Gson 2.10 (transitif dari converter-gson) belum membawa aturan R8: TypeToken generik
# (`object : TypeToken<ApiResponse<Any>>() {}` di ApiCall.kt) butuh Signature.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-dontwarn sun.misc.**

# Stack trace crash release tetap terbaca baris aslinya (de-obfuscate pakai mapping.txt).
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
