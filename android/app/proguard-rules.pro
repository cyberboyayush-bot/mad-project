# kotlinx.serialization and Retrofit ship their own consumer rules.
# Keep generic signatures for Retrofit suspend functions.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
