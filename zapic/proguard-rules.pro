# Keep Java API
-keep class com.zapic.sdk.android.Zapic {
  public *;
}

-keep interface com.zapic.sdk.android.Zapic$AuthenticationHandler {
  public *;
}

-keep class com.zapic.sdk.android.ZapicPlayer {
  public *;
}

# Keep JavaScript API
-keepclassmembers class com.zapic.sdk.android.WebViewJavascriptInterface {
  public *;
}
