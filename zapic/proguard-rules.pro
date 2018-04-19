# Keep Java API
-keep class com.zapic.sdk.android.Zapic {
  public *;
}

# Keep Java API
-keep class com.zapic.sdk.android.ZapicPlayer {
  public *;
}

# Keep JavaScript API
-keepclassmembers class com.zapic.sdk.android.WebViewJavascriptInterface {
  public *;
}
