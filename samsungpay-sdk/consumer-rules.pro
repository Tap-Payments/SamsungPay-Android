# Consumer rules shipped with the Samsung Pay SDK (applied automatically in the host app's R8 build).
# The SDK parses remote JSON (Retrofit + Gson) into these model classes by FIELD NAME. If R8 renames
# the fields, Gson silently returns nulls: e.g. interceptedURL stays null, so a Samsung Wallet
# cancel is never detected and the payment flow hangs.
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, *Annotation*
-keep class com.tap.company.samsungpay_sdk.model.** { *; }
-keep class com.tap.company.samsungpay_sdk.ApiServiceBenefit { *; }
