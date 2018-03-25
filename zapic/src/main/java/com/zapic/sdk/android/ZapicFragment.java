package com.zapic.sdk.android;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public final class ZapicFragment extends Fragment {
    /**
     * Identifies an action request to sign-in using Google Play Games Services.
     */
    private static final int GOOGLE_SIGN_IN_REQUEST = 1000;

    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "ZapicFragment";

    /**
     * The Google Sign-In client.
     */
    @Nullable
    private GoogleSignInClient mGoogleSignInClient;

    /**
     * A value that indicates whether the Zapic JavaScript application has requested to login using
     * the Google Sign-In client.
     */
    private boolean mGoogleSignInRequested;

    /**
     * A strong reference to the {@link WebViewManager} instance.
     */
    @Nullable
    private WebViewManager mWebViewManager;

    /**
     * Creates a new {@link ZapicFragment} instance.
     */
    public ZapicFragment() {
        this.mGoogleSignInClient = null;
        this.mGoogleSignInRequested = false;
        this.mWebViewManager = null;
    }

    /**
     * Creates the Google Sign-In client.
     *
     * @param activity The activity to which the Google Sign-In client is bound.
     * @return The Google Sign-In client.
     * @throws RuntimeException If the Android application's manifest does not have the APP_ID and
     *                          WEB_CLIENT_ID meta-data tags.
     */
    @CheckResult
    @MainThread
    @NonNull
    private static GoogleSignInClient createGoogleSignInClient(@NonNull final Activity activity) {
        final String packageName = activity.getPackageName();
        Bundle metaData;
        try {
            final ApplicationInfo info = activity.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            metaData = info.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Using Zapic requires a metadata tag with the name \"com.google.android.gms.games.APP_ID\" in the application tag of the manifest for " + packageName);
        }

        String appId = metaData.getString("com.google.android.gms.games.APP_ID");
        if (appId != null) {
            appId = appId.trim();
            if (appId.length() == 0) {
                appId = null;
            }
        }

        if (appId == null) {
            throw new RuntimeException("Using Zapic requires a metadata tag with the name \"com.google.android.gms.games.APP_ID\" in the application tag of the manifest for " + packageName);
        }

        String webClientId = metaData.getString("com.google.android.gms.games.WEB_CLIENT_ID");
        if (webClientId != null) {
            webClientId = webClientId.trim();
            if (webClientId.length() == 0) {
                webClientId = null;
            }
        }

        if (webClientId == null) {
            throw new RuntimeException("Using Zapic requires a metadata tag with the name \"com.google.android.gms.games.WEB_CLIENT_ID\" in the application tag of the manifest for " + packageName);
        }

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .requestServerAuthCode(webClientId)
                .build();
        return GoogleSignIn.getClient(activity, options);
    }

    void login() {
        this.mGoogleSignInRequested = true;

        assert this.mGoogleSignInClient != null : "mGoogleSignInClient is null";
        this.startActivityForResult(this.mGoogleSignInClient.getSignInIntent(), GOOGLE_SIGN_IN_REQUEST);
    }

    void logout() {
        this.mGoogleSignInRequested = false;

        assert this.mGoogleSignInClient != null : "mGoogleSignInClient is null";
        this.mGoogleSignInClient.signOut();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onActivityResult");
        }

        if (requestCode == GOOGLE_SIGN_IN_REQUEST) {
            this.mGoogleSignInRequested = false;

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
            try {
                // Interactive sign-in succeeded.
                final String serverAuthCode = task.getResult(ApiException.class).getServerAuthCode();
                this.onLoginSucceeded(serverAuthCode == null ? "" : serverAuthCode);
            } catch (ApiException e) {
                // Interactive sign-in failed; return error.
                this.onLoginFailed(e);
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onAttach(Activity activity) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onAttach");
        }

        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            assert this.mGoogleSignInClient == null : "mGoogleSignInClient is not null";
            this.mGoogleSignInClient = ZapicFragment.createGoogleSignInClient(this.getActivity());
        }
    }

    @Override
    public void onAttach(Context context) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onAttach");
        }

        super.onAttach(context);

        assert this.mGoogleSignInClient == null : "mGoogleSignInClient is not null";
        this.mGoogleSignInClient = ZapicFragment.createGoogleSignInClient(this.getActivity());
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreate");
        }

        super.onCreate(savedInstanceState);

        // This retains the fragment instance when configuration changes occur. This effectively
        // keeps instance variables from being garbage collected. Note that the fragment is still
        // detached from the old activity and attached to the new activity.
        this.setRetainInstance(true);

        assert this.mWebViewManager == null : "mWebViewManager is not null";
        this.mWebViewManager = WebViewManager.getInstance();
        this.mWebViewManager.onFragmentCreated(this);
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDestroy");
        }

        super.onDestroy();

        assert this.mGoogleSignInClient != null : "mGoogleSignInClient is null";
        this.mGoogleSignInClient = null;

        assert this.mWebViewManager != null : "mWebViewManager is null";
        this.mWebViewManager.onFragmentDestroyed(this);

        if (this.mGoogleSignInRequested) {
            // Transfer the sign-in request to another fragment.
            this.mWebViewManager.login();
        }

        this.mWebViewManager = null;
    }

    @Override
    public void onDetach() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDetach");
        }

        super.onDetach();

        this.mGoogleSignInClient = null;
    }

    private void onLoginFailed(@NonNull final Exception exception) {
        this.mGoogleSignInRequested = false;

        String message;
        if (exception instanceof ApiException) {
            ApiException apiException = (ApiException) exception;
            message = String.valueOf(apiException.getStatusCode());
        } else {
            message = "Failed to sign-in to Play Games";
        }

        assert this.mWebViewManager != null : "mWebViewManager is null";
        this.mWebViewManager.loginFailed(message);
    }

    private void onLoginSucceeded(@NonNull final String serverAuthCode) {
        this.mGoogleSignInRequested = false;

        final String packageName = this.getActivity().getApplicationContext().getPackageName();

        assert this.mWebViewManager != null : "mWebViewManager is null";
        this.mWebViewManager.loginSucceeded(packageName, serverAuthCode);
    }

    @Override
    public void onResume() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onResume");
        }

        super.onResume();

        assert this.mGoogleSignInClient != null : "mGoogleSignInClient is null";
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this.getActivity());
        if (account == null) {
            this.mGoogleSignInClient.silentSignIn().addOnCompleteListener(this.getActivity(), new OnCompleteListener<GoogleSignInAccount>() {
                @MainThread
                @Override
                public void onComplete(@NonNull final Task<GoogleSignInAccount> task) {
                    try {
                        // Silent sign-in succeeded.
                        final String serverAuthCode = task.getResult(ApiException.class).getServerAuthCode();
                        ZapicFragment.this.onLoginSucceeded(serverAuthCode == null ? "" : serverAuthCode);
                    } catch (ApiException e) {
                        if (ZapicFragment.this.mGoogleSignInRequested && ZapicFragment.this.mGoogleSignInClient != null) {
                            // Silent sign-in failed; try interactive sign-in.
                            ZapicFragment.this.startActivityForResult(ZapicFragment.this.mGoogleSignInClient.getSignInIntent(), GOOGLE_SIGN_IN_REQUEST);
                        }
                    }
                }
            });
        } else if (this.mGoogleSignInRequested) {
            final String serverAuthCode = account.getServerAuthCode();
            this.onLoginSucceeded(serverAuthCode == null ? "" : serverAuthCode);
        }
    }
}
