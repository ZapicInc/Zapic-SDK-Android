package com.zapic.android.demo;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.ResultTransform;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;

public class MainActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LoginFragment.OnLoginListener,
        LogoutFragment.OnLogoutListener {

    private static final int RC_SIGN_IN = 9001;
    private GoogleApiClient mGoogleApiClient;

    private void connect() {
        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect(GoogleApiClient.SIGN_IN_MODE_OPTIONAL);
        }
    }

    private void disconnect() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    private void login() {
        connect();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
    }

    private void logout() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {

            if (mGoogleApiClient.hasConnectedApi(Games.API)) {
                Games.signOut(mGoogleApiClient);
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
//                Games.signOut(mGoogleApiClient)
//                        .then(new ResultTransform<Status, Status>() {
//                            @Nullable
//                            @Override
//                            public PendingResult<Status> onSuccess(@NonNull final Status status) {
//                                return Auth.GoogleSignInApi.signOut(mGoogleApiClient);
//                            }
//                        })
//                        .andFinally(new ResultCallbacks<Status>() {
//                            @Override
//                            public void onSuccess(@NonNull Status status) {
//                            }
//
//                            @Override
//                            public void onFailure(@NonNull Status status) {
//                            }
//                        });
            }
            mGoogleApiClient.disconnect();
        }
    }

    private void showLogin() {
        LoginFragment fragment = new LoginFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    private void showLogout() {
        LogoutFragment fragment = new LogoutFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    @Override
    public void onLogin() {
        login();

    }

    @Override
    public void onLogout() {
        logout();
        showLogin();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from
        //   GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (googleSignInResult != null) {
                Status status = googleSignInResult.getStatus();
                if (googleSignInResult.isSuccess()) {

                    Log.d("ZAPIC", "Success with silentSignIn: " + status);

                    GoogleSignInAccount account = googleSignInResult.getSignInAccount();
                    if (account != null) {
                        String serverAuthCode = account.getServerAuthCode();
                        Log.i("ZAPIC", serverAuthCode);
                    }

                    showLogout();
                } else {
                    Log.e("ZAPIC", "Error with silentSignIn: " + status);
                    showLogin();
                }
            }
            if (googleSignInResult.isSuccess()) {
                GoogleSignInAccount acct = googleSignInResult.getSignInAccount();
                // Get account information
                String mFullName = acct.getDisplayName();
                String mEmail = acct.getEmail();
            }
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        WorkingFragment fragment = new WorkingFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .requestServerAuthCode(getString(R.string.webclient_id))
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Games.API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, options)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnect();
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.hasConnectedApi(Games.API)) {
                Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient).setResultCallback(
                    new ResultCallback<GoogleSignInResult>() {
                        @Override
                        public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                            if (googleSignInResult != null) {
                                Status status = googleSignInResult.getStatus();
                                if (googleSignInResult.isSuccess()) {
                                    Log.d("ZAPIC", "Success with silentSignIn: " + status);

                                    GoogleSignInAccount account = googleSignInResult.getSignInAccount();
                                    if (account != null) {
                                        String serverAuthCode = account.getServerAuthCode();
                                        Log.i("ZAPIC", serverAuthCode);
                                    }

                                    showLogout();
                                } else {
                                    Log.e("ZAPIC", "Error with silentSignIn: " + status);
                                    showLogin();
                                }
                            }
                        }
                    }
                );
            } else {
                login();
            }
        }
    }

    @Override
    public void onConnectionSuspended(final int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {

        if (connectionResult.hasResolution()) {
            try {
                // !!!
                connectionResult.startResolutionForResult(this, 1000);
            } catch (IntentSender.SendIntentException e) {
                showLogin();

            }
        }
    }
}
