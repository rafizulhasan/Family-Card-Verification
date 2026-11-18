# Add project specific ProGuard rules here.

# Keep JNI bridge classes, native pointer fields, and C++ callbacks for Tesseract & Leptonica
-keep class com.googlecode.tesseract.android.** { *; }
-keep class com.googlecode.leptonica.android.** { *; }

# Keep PDFBox-Android reflection-instantiated security handlers & silence optional desktop/crypto warnings
-keep class * extends com.tom_roush.pdfbox.pdmodel.encryption.SecurityHandler {
    public <init>(...);
}
-dontwarn org.bouncycastle.**
-dontwarn com.gemalto.jp2.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
