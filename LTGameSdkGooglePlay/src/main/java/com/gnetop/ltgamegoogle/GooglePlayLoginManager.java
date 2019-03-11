package com.gnetop.ltgamegoogle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.gnetop.ltgamecommon.impl.OnLoginSuccessListener;
import com.gnetop.ltgamecommon.login.LoginBackManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.Map;
import java.util.WeakHashMap;

public class GooglePlayLoginManager {

    private static final String TAG = GooglePlayLoginManager.class.getSimpleName();

    public static void initGoogle(Activity context, String clientID, int selfRequestCode) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientID)
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        context.startActivityForResult(signInIntent, selfRequestCode);
    }


    public static void onActivityResult(int requestCode, Intent data, int selfRequestCode,
                                        Context context,String LTAppID,
                                        String LTAppKey,OnLoginSuccessListener mListener) {
        if (requestCode == selfRequestCode) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(context,LTAppID,LTAppKey,task,mListener);
        }
    }


    private static void handleSignInResult(Context context,String LTAppID,
                                           String LTAppKey, @NonNull Task<GoogleSignInAccount> completedTask,
                                           OnLoginSuccessListener mListener) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account.getIdToken();
            Log.e(TAG,idToken);
            Map<String,Object> map=new WeakHashMap<>();
            map.put("access_token",idToken);
            map.put("platform",2);
            LoginBackManager.googleLogin(context,  LTAppID,
                    LTAppKey, map, mListener);
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }


}
