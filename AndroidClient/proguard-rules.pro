# Various duplicate classes due to my packaging
-dontwarn aQute.bnd.annotation.**
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**
-dontwarn org.freedesktop.dbus.**
-dontwarn org.mpris.**
-dontwarn javax.sound.sampled.**
-dontwarn org.osgi.framework.**
-dontwarn dev.blackilykat.jpasimple.**
-dontwarn javax.annotation.Nonnull
-dontwarn javax.annotation.Nullable
-dontwarn javax.naming.OperationNotSupportedException
-dontwarn edu.umd.cs.findbugs.annotations.Nullable
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn com.google.errorprone.annotations.InlineMe

# for jackson's TypeReference
-keepattributes Signature,EnclosingMethod
# for jackson annotations
-keepattributes *Annotation*
# for the jackson parameter names module
-keepattributes MethodParameters

-keep @interface com.fasterxml.jackson.annotation.** { *; }
-keep class dev.blackilykat.pmp.event.EventSource { *; }
-keep class * extends dev.blackilykat.pmp.event.EventSource { *; }
-keep class dev.blackilykat.pmp.messages.Message { *; }
-keep class * extends dev.blackilykat.pmp.messages.Message { *; }
-keep class * extends dev.blackilykat.pmp.storage.Storage { *; }
-keep class dev.blackilykat.pmp.client.Header { *; }
-keep class dev.blackilykat.pmp.client.Filter { *; }
-keep class dev.blackilykat.pmp.client.FilterOption { *; }
-keep class dev.blackilykat.pmp.client.Track { *; }
-keep class dev.blackilykat.pmp.client.Track$PlaybackInfo { *; }
-keep class dev.blackilykat.pmp.Action$Type { *; }
-keep class dev.blackilykat.pmp.client.Server$TrackElement { *; }
-keep enum * { *; }
-keep class * extends java.lang.Record { *; }
-keep class * implements java.io.Serializable { *; }
-keep class * extends com.fasterxml.jackson.core.type.TypeReference { *; }
-keep class com.fasterxml.jackson.core.type.TypeReference



##### COMMON
# Preserve some attributes that may be required for reflection.
-keepattributes AnnotationDefault,
                EnclosingMethod,
                InnerClasses,
                RuntimeVisibleAnnotations,
                RuntimeVisibleParameterAnnotations,
                RuntimeVisibleTypeAnnotations,
                Signature

-keep public class com.google.vending.licensing.ILicensingService
-keep public class com.android.vending.licensing.ILicensingService
-keep public class com.google.android.vending.licensing.ILicensingService
-dontnote com.android.vending.licensing.ILicensingService
-dontnote com.google.vending.licensing.ILicensingService
-dontnote com.google.android.vending.licensing.ILicensingService

# For native methods, see https://www.guardsquare.com/manual/configuration/examples#native
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep setters in Views so that animations can still work.
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}

# We want to keep methods in Activity that could be used in the XML attribute onClick.
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# For enumeration classes, see https://www.guardsquare.com/manual/configuration/examples#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Preserve annotated Javascript interface methods.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# The support libraries contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version. We know about them, and they are safe.
-dontnote android.support.**
-dontnote androidx.**
-dontwarn android.support.**
-dontwarn androidx.**

# Understand the @Keep support annotation.
-keep class android.support.annotation.Keep

-keep @android.support.annotation.Keep class * {*;}

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <methods>;
}

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <fields>;
}

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <init>(...);
}

# These classes are duplicated between android.jar and org.apache.http.legacy.jar.
-dontnote org.apache.http.**
-dontnote android.net.http.**

# These classes are duplicated between android.jar and core-lambda-stubs.jar.
-dontnote java.lang.invoke.**
