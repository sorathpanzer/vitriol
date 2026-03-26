# --- General ---
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.lifecycle.**
-dontwarn androidx.annotation.**

# Keep Kotlin metadata for reflection and serialization
-keepnames class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; }
-keepnames class kotlin.reflect.** { *; }
-keepnames class kotlin.jvm.internal.** { *; }

# Keep Parcelable implementations (needed for Android)
-keepnames class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# --- Keep your app's specific classes for reflection and serialization ---

# Keep your app settings data classes and annotations
-keepnames class app.vitriol.data.settings.AppSettings { *; }
-keepclassmembers class app.vitriol.data.settings.AppSettings { *; }
-keepnames @app.vitriol.data.settings.Setting class * { *; }
-keepclasseswithmembers class * {
    @app.vitriol.data.settings.Setting <fields>;
}

-keepnames class app.vitriol.data.settings.SettingsManager { *; }
-keepnames class app.vitriol.ui.screens.SettingsScreenKt { *; }

# Keep all Setting annotations and enums used in annotations
-keepnames @interface app.vitriol.data.settings.Setting
-keepclassmembers enum app.vitriol.data.settings.SettingCategory { *; }
-keepclassmembers enum app.vitriol.data.settings.SettingType { *; }

# Keep annotations & runtime visible annotations for reflection & tools
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations, Signature, EnclosingMethod, InnerClasses

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
