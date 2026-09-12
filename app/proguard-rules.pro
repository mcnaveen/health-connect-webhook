# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepclassmembers class com.feedbackjar.sdk.internal.MetadataCollector {
    public *;
}

# gRPC / protobuf lite
#
# Keep generated app messages (also covered by GeneratedMessageLite below).
-keep class com.hcwebhook.app.proto.v1.** { *; }
-keepclassmembers class com.hcwebhook.app.proto.v1.** { *; }
#
# protobuf-javalite uses reflection over field names (e.g. seconds_ / nanos_ on
# google.protobuf.Timestamp and Duration). R8 renaming those fields causes:
#   Field seconds_ for com.google.protobuf.<obfuscated> not found
# Official rule: https://github.com/protocolbuffers/protobuf/blob/main/java/lite.md
# Do NOT add allowobfuscation — that reintroduces the failure (#77).
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-dontwarn com.google.protobuf.**
-dontwarn io.grpc.**
-dontwarn javax.annotation.**
