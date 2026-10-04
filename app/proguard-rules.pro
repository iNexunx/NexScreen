# Keep annotations and generic signatures so reflection-based
# libraries (Shizuku, DataStore, Compose tooling) keep working.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ------------------------------------------------------------------
# Enum names are part of the persisted data format.
# SettingsStore writes `ThemeMode.DARK_PURE.name` into DataStore and
# reads it back with `valueOf()`. Without this rule R8 renames the
# constants, every previously saved preference stops matching and the
# app silently falls back to defaults (loses user settings on upgrade).
# ------------------------------------------------------------------
-keepclassmembers enum nx.screen.ds.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep enum nx.screen.ds.** {
    *;
}

# ------------------------------------------------------------------
# Shizuku: talks over AIDL and resolves some classes reflectively.
# ------------------------------------------------------------------
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keep class rikka.shizuku.ShizukuProvider { *; }
-dontwarn rikka.shizuku.**
-dontwarn moe.shizuku.**

# ------------------------------------------------------------------
# AndroidX DataStore preferences reads its schema from generated
# classes; keep the serializer surface intact.
# ------------------------------------------------------------------
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ------------------------------------------------------------------
# Manifest-declared components. Kept explicitly so shrinking can never
# strip them from the merged manifest.
# ------------------------------------------------------------------
-keep class nx.screen.ds.App { *; }
-keep class nx.screen.ds.MainActivity { *; }
-keep class nx.screen.ds.core.ProfileService { *; }
-keep class nx.screen.ds.core.BootReceiver { *; }

# ViewModels are instantiated reflectively by the Compose viewModel()
# factory, which relies on the no-arg/Application constructors.
-keep class nx.screen.ds.ui.HomeViewModel { *; }
-keep class nx.screen.ds.ui.SettingsViewModel { *; }
-keep class nx.screen.ds.ui.AppsViewModel { *; }

# ------------------------------------------------------------------
# Kotlin coroutines / stdlib internals.
# ------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
