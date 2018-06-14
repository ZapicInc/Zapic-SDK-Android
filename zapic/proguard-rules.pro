# Keep Java API
-keep class com.zapic.sdk.android.Zapic {
  public *;
}

-keep class com.zapic.sdk.android.ZapicPages {
  public *;
}

-keep class com.zapic.sdk.android.ZapicPlayer {
  public *;
}

-keep interface com.zapic.sdk.android.Zapic$AuthenticationHandler {
  public *;
}

# Keep JavaScript API
-keepclassmembers class com.zapic.sdk.android.WebViewJavascriptBridge {
  public *;
}
