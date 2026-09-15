# kotlinx.serialization ships its own consumer rules with the artifact.
# Supabase/Ktor models are (de)serialized reflectively via the serializer plugin.
-keep,includedescriptorclasses class com.archeryscore.app.**$$serializer { *; }
-keepclasseswithmembers class com.archeryscore.app.** { companion object; }