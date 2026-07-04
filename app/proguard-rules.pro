# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class pk.fuelgo.customer.**$$serializer { *; }
-keepclassmembers class pk.fuelgo.customer.** { *** Companion; }
-keepclasseswithmembers class pk.fuelgo.customer.** { kotlinx.serialization.KSerializer serializer(...); }
