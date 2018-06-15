# Keep Zapic Java APIs
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

# Keep Zapic JavaScript API
-keepclassmembers class com.zapic.sdk.android.WebViewJavascriptBridge {
  public *;
}

# Keep Java APIs used by reflection
-keepnames class android.support.v4.app.Fragment
