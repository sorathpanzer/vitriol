# --- General warnings ---
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.lifecycle.**
-dontwarn androidx.annotation.**

# Keep AppSettings data class and fields
-keep class app.vitriol.data.settings.AppSettings { *; }
