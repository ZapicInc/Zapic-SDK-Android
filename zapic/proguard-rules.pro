# Keep Java API
-keep class com.zapic.android.sdk.Zapic {
  public *;
}

# Keep JavaScript API
-keepclassmembers class com.zapic.android.sdk.AppJavaScriptBridge {
  public *;
}
