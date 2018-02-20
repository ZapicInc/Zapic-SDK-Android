package com.zapic.android.sdk;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * A {@link Fragment} that runs the Zapic JavaScript application in the background.
 * <p>
 * Use the {@link Zapic#attachFragment(Activity)} method to create and attach instances of this
 * fragment to your game's activity. Alternatively, use the {@link #createInstance()} factory method
 * to create instances of this fragment.
 * <p>
 * The Zapic JavaScript application runs in a {@link WebView}. Generally, the {@link WebView} runs
 * for the lifetime of the Android application. While the game is in focus, the {@link WebView} runs
 * in the background (managed by one or more {@link ZapicFragment}s attached to the game's
 * activities). The Zapic JavaScript application processes events and receives notifications while
 * running in the background. When the player shifts focus to Zapic, the {@link WebView} moves to
 * the foreground and is presented by a {@link ZapicActivity}. When the player shifts focus back to
 * the game, the {@link ZapicActivity} finishes and the {@link WebView} returns to the background
 * (again managed by one or more {@link ZapicFragment}s attached to the game's activities).
 *
 * @author Kyle Dodson
 * @since 1.0.0
 */
public final class ZapicFragment extends Fragment {
    /**
     * A utility class that coordinates the Zapic JavaScript application with the various views
     * ({@link ZapicActivity}, {@link ZapicFragment}, and {@link AppPageFragment} instances).
     * <p>
     * This starts and stops the Zapic JavaScript application when the first {@link ZapicFragment}
     * is created and the last {@link ZapicFragment} is destroyed, respectively.
     * <p>
     * This ensures at most one {@link ZapicActivity} is running to prevent adding the
     * {@link WebView} to multiple view hierarchies.
     * <p>
     * This adds and removes the {@link WebView} to and from the {@link AppPageFragment}.
     * <p>
     * Use the {@link #getInstance()} method to get the singleton instance.
     *
     * @author Kyle Dodson
     * @see App
     * @see AppPageFragment
     * @see ZapicActivity
     * @see ZapicFragment
     * @since 1.0.0
     */
    private static final class ZapicViewManager implements App.InteractionListener {
        /**
         * A weak reference to the {@link ZapicViewManager} "singleton instance". Technically, as a
         * weak reference, this is not a singleton instance as defined by the software design
         * pattern. The weak reference is required, though. It ensures we don't leak the Android
         * application's context. In practice, we cannot discern this implementation difference.
         * {@link ZapicFragment} instances keep a strong reference to this {@link ZapicViewManager}
         * instance. As such, this {@link ZapicViewManager} instance can only be garbage collected
         * after all of our {@link ZapicFragment} instances have been garbage collected. This is no
         * different from the state at initial launch.
         */
        @NonNull
        private static WeakReference<ZapicViewManager> INSTANCE = new WeakReference<>(null);

        /**
         * Gets the {@link ZapicViewManager} singleton instance.
         *
         * @return The {@link ZapicViewManager} singleton instance.
         */
        @MainThread
        @NonNull
        static ZapicViewManager getInstance() {
            ZapicViewManager instance = ZapicViewManager.INSTANCE.get();
            if (instance == null) {
                instance = new ZapicViewManager();
                ZapicViewManager.INSTANCE = new WeakReference<>(instance);
            }

            return instance;
        }

        /**
         * The {@link ZapicActivity} instance.
         */
        @Nullable
        private ZapicActivity mActivity;

        /**
         * The Zapic JavaScript application.
         */
        @NonNull
        private final App mApp;

        /**
         * The list of {@link ZapicFragment} instances.
         */
        @NonNull
        private final ArrayList<ZapicFragment> mFragments;

        /**
         * Creates a new {@link ZapicViewManager} instance.
         */
        @MainThread
        private ZapicViewManager() {
            this.mActivity = null;
            this.mApp = new App(this);
            this.mFragments = new ArrayList<>();
        }

        @Override
        public void login() {
            final int size = this.mFragments.size();
            if (size == 0) {
                return;
            }

            for (int i = size - 1; i >= 0; --i) {
                final ZapicFragment fragment = this.mFragments.get(i);
                if (fragment.mGoogleSignInClient != null) {
                    fragment.mGoogleSignInRequested = true;
                    fragment.startActivityForResult(fragment.mGoogleSignInClient.getSignInIntent(), ZapicFragment.RC_GOOGLE_SIGN_IN);
                    return;
                }
            }

            final ZapicFragment fragment = this.mFragments.get(size - 1);
            fragment.mGoogleSignInRequested = true;
        }

        @Override
        public void logout() {
            final int size = this.mFragments.size();
            if (size == 0) {
                return;
            }

            GoogleSignInClient signInClient = null;
            for (int i = 0; i < size; ++i) {
                final ZapicFragment fragment = this.mFragments.get(i);
                if (fragment.mGoogleSignInClient != null && signInClient == null) {
                    signInClient = fragment.mGoogleSignInClient;
                }

                fragment.mGoogleSignInRequested = false;
            }

            if (signInClient != null) {
                signInClient.signOut();
            }
        }

        /**
         * Called when a {@link ZapicFragment} is attached to an {@link Activity}. This checks to
         * see if the {@link Activity} is a {@link ZapicActivity}. The previous
         * {@link ZapicActivity}, if any, is closed to prevent adding the {@link WebView} to
         * multiple view hierarchies.
         *
         * @param fragment The {@link ZapicFragment} instance.
         */
        @MainThread
        private void onAttached(@NonNull final ZapicFragment fragment) {
            final Activity activity = fragment.getActivity();
            if (!(activity instanceof ZapicActivity)) {
                return;
            }

            final ZapicActivity zapicActivity = (ZapicActivity) activity;
            if (this.mActivity == zapicActivity) {
                return;
            }

            if (this.mActivity != null) {
                this.mActivity.close();
            }

            this.mActivity = (ZapicActivity) activity;
        }

        /**
         * Called when a {@link ZapicFragment} is created. This adds the {@link ZapicFragment} to
         * {@link #mFragments} and, if this is the first fragment, starts the Zapic JavaScript
         * application.
         *
         * @param fragment The {@link ZapicFragment} instance.
         */
        @MainThread
        private void onCreated(@NonNull final ZapicFragment fragment) {
            this.onAttached(fragment);

            this.mFragments.add(fragment);
            if (this.mFragments.size() == 1) {
                // Start the application if this is the first fragment.
                this.mApp.start(fragment.getActivity());
            }
        }

        /**
         * Called when a {@link ZapicFragment} is destroyed. This removes the {@link ZapicFragment}
         * from {@link #mFragments} and, if this is the last fragment, stops the Zapic JavaScript
         * application.
         *
         * @param fragment The {@link ZapicFragment} instance.
         */
        @MainThread
        private void onDestroyed(@NonNull final ZapicFragment fragment) {
            this.onDetached(fragment);

            this.mFragments.remove(fragment);
            if (this.mFragments.size() == 0) {
                // Stop the application if this is the last fragment.
                this.mApp.stop();
            } else {
                final Activity activity = fragment.getActivity();
                if (!(activity instanceof ZapicActivity)) {
                    return;
                }

                if (this.mActivity != null) {
                    return;
                }

                final WebView webView = this.mApp.getWebView();
                if (webView == null) {
                    return;
                }

                webView.evaluateJavascript("window.zapic.dispatch({ type: 'CLOSE_PAGE' })", null);
            }
        }

        /**
         * Called when a {@link ZapicFragment} is detached from an {@link Activity}. This checks to
         * see if the {@link Activity} is a {@link ZapicActivity}.
         *
         * @param fragment The {@link ZapicFragment} instance.
         */
        @MainThread
        private void onDetached(@NonNull final ZapicFragment fragment) {
            final Activity activity = fragment.getActivity();
            if (!(activity instanceof ZapicActivity)) {
                return;
            }

            final ZapicActivity zapicActivity = (ZapicActivity) activity;
            if (this.mActivity != zapicActivity) {
                return;
            }

            this.mActivity = null;
        }

        @Override
        public void onStateChanged(App.State state) {
            switch (state) {
                case LOADED: {
                    break;
                }
                case NOT_LOADED: {
                    if (this.mActivity != null && this.mFragments.size() > 0) {
                        if (this.mApp.getConnected()) {
                            this.mActivity.openLoadingPage();
                            this.mApp.start(this.mActivity.getApplicationContext());
                        } else {
                            this.mActivity.openOfflinePage();
                        }
                    }

                    break;
                }
                case NOT_READY: {
                    if (this.mActivity != null) {
                        this.mActivity.close();
                    }

                    break;
                }
                case READY: {
                    if (this.mActivity != null) {
                        this.mActivity.openAppPage();
                    }

                    break;
                }
                case STARTED: {
                    if (this.mActivity != null) {
                        final WebView webView = this.mApp.getWebView();
                        if (webView == null) {
                            return;
                        }

                        String page = this.mActivity.getIntent().getStringExtra("page");
                        if (page == null) {
                            page = "default";
                        }

                        final String escapedPage = page.replace("'", "\\'");
                        webView.evaluateJavascript("window.zapic.dispatch({ type: 'OPEN_PAGE', payload: '" + escapedPage + "' })", null);
                    }

                    break;
                }
                default: {
                    break;
                }
            }
        }

        @Override
        public void toast(@NonNull final String message) {
            if (this.mActivity != null) {
                Toast.makeText(this.mActivity.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * The request code that identifies an intent to login using the Google Sign-In client.
     */
    private static final int RC_GOOGLE_SIGN_IN = 9001;

    /**
     * The tag used to identify log messages.
     */
    @NonNull
    private static final String TAG = "ZapicFragment";

    /**
     * Creates the Google Sign-In client.
     *
     * @param activity The activity to which the Google Sign-In client is bound.
     * @return The Google Sign-In client.
     * @throws RuntimeException If the Android application's manifest does not have the APP_ID and
     * WEB_CLIENT_ID meta-data tags.
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

    /**
     * The broadcast receiver that relays network connectivity changes.
     */
    @Nullable
    private ConnectivityBroadcastReceiver mConnectivityBroadcastReceiver;

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
     * The view manager.
     */
    @Nullable
    private ZapicViewManager mViewManager;

    /**
     * Creates a new {@link ZapicFragment} instance.
     */
    @MainThread
    public ZapicFragment() {
        this.mConnectivityBroadcastReceiver = null;
        this.mGoogleSignInClient = null;
        this.mGoogleSignInRequested = false;
        this.mViewManager = null;
    }

    /**
     * Creates a new {@link ZapicFragment} instance.
     *
     * @return The new {@link ZapicFragment} instance.
     */
    @CheckResult
    @MainThread
    @NonNull
    public static ZapicFragment createInstance() {
        return new ZapicFragment();
    }

    /**
     * Gets the Zapic JavaScript application.
     *
     * @return The Zapic JavaScript application or {@code null} if called before the
     * {@link #onCreate(Bundle)} lifecycle method or after the {@link #onDestroy()} lifecycle
     * method.
     */
    @CheckResult
    @MainThread
    @Nullable
    App getApp() {
        return this.mViewManager == null ? null : this.mViewManager.mApp;
    }

    @MainThread
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onAttach");
        }

        super.onActivityResult(requestCode, resultCode, intent);

        if (ZapicFragment.this.mViewManager != null && requestCode == ZapicFragment.RC_GOOGLE_SIGN_IN) {
            ZapicFragment.this.mGoogleSignInRequested = false;

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

    @MainThread
    @Override
    public void onAttach(final Context context) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onAttach");
        }

        super.onAttach(context);

        if (this.mViewManager != null) {
            this.mViewManager.onAttached(this);

            assert this.mConnectivityBroadcastReceiver == null : "mConnectivityBroadcastReceiver != null";
            this.mConnectivityBroadcastReceiver = ConnectivityManagerUtilities.registerConnectivityBroadcastReceiver(this.getActivity(), this.mViewManager.mApp);
            this.mConnectivityBroadcastReceiver.notify(this.getActivity());

            assert this.mGoogleSignInClient == null : "mGoogleSignInClient != null";
            this.mGoogleSignInClient = ZapicFragment.createGoogleSignInClient(this.getActivity());
        }
    }

    @MainThread
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreate");
        }

        super.onCreate(savedInstanceState);

        // This retains the fragment instance when configuration changes occur. This effectively
        // keeps instance variables from being garbage collected. Note that the fragment is still
        // detached from the old activity and attached to the new activity.
        this.setRetainInstance(true);

        assert this.mViewManager == null : "mViewManager != null";
        this.mViewManager = ZapicViewManager.getInstance();
        this.mViewManager.onCreated(this);

        assert this.mConnectivityBroadcastReceiver == null : "mConnectivityBroadcastReceiver != null";
        this.mConnectivityBroadcastReceiver = ConnectivityManagerUtilities.registerConnectivityBroadcastReceiver(this.getActivity(), this.mViewManager.mApp);
        this.mConnectivityBroadcastReceiver.notify(this.getActivity());

        assert this.mGoogleSignInClient == null : "mGoogleSignInClient != null";
        this.mGoogleSignInClient = ZapicFragment.createGoogleSignInClient(this.getActivity());
    }

    @MainThread
    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDestroy");
        }

        super.onDestroy();

        assert this.mGoogleSignInClient != null : "mGoogleSignInClient == null";
        this.mGoogleSignInClient = null;

        assert this.mConnectivityBroadcastReceiver != null : "mConnectivityBroadcastReceiver == null";
        ConnectivityManagerUtilities.unregisterConnectivityBroadcastReceiver(this.getActivity(), this.mConnectivityBroadcastReceiver);
        this.mConnectivityBroadcastReceiver = null;

        assert this.mViewManager != null : "mViewManager == null";
        ZapicViewManager viewManager = this.mViewManager;
        this.mViewManager.onDestroyed(this);
        this.mViewManager = null;

        if (this.mGoogleSignInRequested) {
            // Transfer the sign-in request to another fragment.
            viewManager.login();
        }
    }

    @MainThread
    @Override
    public void onDetach() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDetach");
        }

        super.onDetach();

        if (this.mViewManager != null) {
            assert this.mGoogleSignInClient != null;
            this.mGoogleSignInClient = null;

            assert this.mConnectivityBroadcastReceiver != null : "mConnectivityBroadcastReceiver == null";
            ConnectivityManagerUtilities.unregisterConnectivityBroadcastReceiver(this.getActivity(), this.mConnectivityBroadcastReceiver);
            this.mConnectivityBroadcastReceiver = null;

            this.mViewManager.onDetached(this);
        }
    }

    @MainThread
    private void onLoginFailed(@NonNull final Exception exception) {
        final ZapicViewManager viewManager = ZapicFragment.this.mViewManager;
        if (viewManager == null) {
            return;
        }

        final App app = viewManager.mApp;
        final WebView webView = app.getWebView();
        if (webView == null) {
            return;
        }

        String payload;
        if (exception instanceof ApiException) {
            ApiException apiException = (ApiException) exception;
            payload = "'" + String.valueOf(apiException.getStatusCode()) + "'";
        } else {
            payload = "'Failed to sign-in to Play Games'";
        }

        this.mGoogleSignInRequested = false;
        webView.evaluateJavascript("window.zapic.dispatch({ type: 'LOGIN_WITH_PLAY_GAME_SERVICES', payload: " + payload + " })", null);
    }

    @MainThread
    private void onLoginSucceeded(@NonNull final String serverAuthCode) {
        final ZapicViewManager viewManager = ZapicFragment.this.mViewManager;
        if (viewManager == null) {
            return;
        }

        final App app = viewManager.mApp;
        final WebView webView = app.getWebView();
        if (webView == null) {
            return;
        }

        final String packageName = this.getActivity().getPackageName();
        final String payload = "{ authCode: '" + serverAuthCode.replace("'", "\\'") + "', packageName: '" + packageName.replace("'", "\\'") + "' }";

        this.mGoogleSignInRequested = false;
        webView.evaluateJavascript("window.zapic.dispatch({ type: 'LOGIN_WITH_PLAY_GAME_SERVICES', payload: " + payload + " })", null);
    }

    @MainThread
    @Override
    public void onResume() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onResume");
        }

        super.onResume();

        assert this.mGoogleSignInClient != null : "mGoogleSignInClient == null";
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this.getActivity());
        if (account == null) {
            this.mGoogleSignInClient.silentSignIn().addOnCompleteListener(this.getActivity(), new OnCompleteListener<GoogleSignInAccount>() {
                @MainThread
                @Override
                public void onComplete(@NonNull final Task<GoogleSignInAccount> task) {
                    if (ZapicFragment.this.mViewManager != null) {
                        try {
                            // Silent sign-in succeeded.
                            final String serverAuthCode = task.getResult(ApiException.class).getServerAuthCode();
                            ZapicFragment.this.onLoginSucceeded(serverAuthCode == null ? "" : serverAuthCode);
                        } catch (ApiException e) {
                            if (ZapicFragment.this.mGoogleSignInRequested && ZapicFragment.this.mGoogleSignInClient != null) {
                                // Silent sign-in failed; try interactive sign-in.
                                ZapicFragment.this.startActivityForResult(ZapicFragment.this.mGoogleSignInClient.getSignInIntent(), ZapicFragment.RC_GOOGLE_SIGN_IN);
                            }
                        }
                    }
                }
            });
        } else if (this.mGoogleSignInRequested) {
            final String serverAuthCode = account.getServerAuthCode();
            this.onLoginSucceeded(serverAuthCode == null ? "" : serverAuthCode);
        }
    }

    //region Lifecycle Events

    // onAttach

    // onCreate

    @CheckResult
    @MainThread
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateView");
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @MainThread
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onActivityCreated");
        }

        super.onActivityCreated(savedInstanceState);
    }

    @MainThread
    @Override
    public void onStart() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStart");
        }

        super.onStart();
    }

    // onResume

    @MainThread
    @Override
    public void onPause() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPause");
        }

        super.onPause();
    }

    @MainThread
    @Override
    public void onStop() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onStop");
        }

        super.onStop();
    }

    @MainThread
    @Override
    public void onDestroyView() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDestroyView");
        }

        super.onDestroyView();
    }

    // onDestroy

    // onDetach

    //endregion
}
