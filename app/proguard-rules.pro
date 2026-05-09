# ── Kotlin Serialization ──────────────────────────────────────────────
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <2>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static ** INSTANCE;
}

-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.example.aapremote.model.**$$serializer { *; }
-keepclassmembers class com.example.aapremote.model.** {
    *** Companion;
}

# ── Retrofit ─────────────────────────────────────────────────────────
-keepattributes Signature,Exceptions,InnerClasses,EnclosingMethod
-keepattributes *Annotation*

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ── OkHttp ───────────────────────────────────────────────────────────
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── Tink ─────────────────────────────────────────────────────────────
-keep class com.google.crypto.tink.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# ── Koin ─────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-keepclassmembers class * {
    public <init>(...);
}

# ── Markdown Renderer ────────────────────────────────────────────────
-keep class com.mikepenz.markdown.** { *; }

# ── App models (Retrofit response types need full generic info) ──────
-keep class com.example.aapremote.model.** { *; }
-keep class com.example.aapremote.network.** { *; }
-keep class com.example.aapremote.assistant.llm.** { *; }
