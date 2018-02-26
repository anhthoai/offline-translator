# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

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

# Preserve Apertium offline translation
-dontwarn com.sun.org.apache.bcel.internal.**
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.tools.**
-dontwarn javax.xml.stream.**
-dontwarn org.apertium.ApertiumGUI
-keepattributes InnerClasses
-keep class org.apertium.**
-keepclassmembers class * {
    public *; protected *; private *;
}

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** e(...);
    public static *** i(...);
    public static *** v(...);
    public static *** w(...);
    public static *** wtf(...);
}

# Remove call to whitelist device as a test device for Admob
-assumenosideeffects class com.google.android.gms.ads.AdRequest.Builder {
    public static *** addTestDevice(...);
}

-dontwarn kotlin.**
