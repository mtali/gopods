# Keep line numbers so release crash reports stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Retrofit. R8 full mode strips generic signatures from classes that are not kept,
# and suspend functions carry their type argument through Continuation.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# kotlinx.serialization. The generated serializers are reached reflectively through
# the companion, so the annotated classes and their serializers have to survive.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.colisa.podplay.**$$serializer { *; }
-keepclassmembers class com.colisa.podplay.** {
    *** Companion;
}
-keepclasseswithmembers class com.colisa.podplay.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Data classes carried over the wire and into DataStore.
-keep class com.colisa.podplay.core.api.models.** { *; }
-keep class com.colisa.podplay.core.models.** { *; }

# Navigation 3 keys are serialized into the saved back stack.
-keep class com.colisa.podplay.features.**.navigation.*NavKey { *; }
