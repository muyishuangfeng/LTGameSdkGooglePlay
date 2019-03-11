package com.gnetop.ltgamegoogle;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.gnetop.ltgamecommon.impl.OnCreateOrderListener;
import com.gnetop.ltgamecommon.impl.OnGoogleInitListener;
import com.gnetop.ltgamecommon.impl.OnGooglePlayResultListener;
import com.gnetop.ltgamecommon.impl.OnGoogleResultListener;
import com.gnetop.ltgamecommon.login.LoginBackManager;
import com.gnetop.ltgamecommon.model.GoogleModel;
import com.gnetop.ltgamegoogle.util.IabHelper;
import com.gnetop.ltgamegoogle.util.IabResult;
import com.gnetop.ltgamegoogle.util.Inventory;
import com.gnetop.ltgamegoogle.util.Purchase;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class GooglePlayManager {
    private static final String TAG = GooglePlayManager.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    private static IabHelper mHelper;
    private static String payload;

    /**
     * 初始化google
     */
    public static void initGooglePlay(Context context, String publicKey,
                                      final String LTAppID, final String LTAppKey,
                                      final String packageId, final Map<String, Object> params,
                                      final String gid,
                                      final OnGoogleInitListener mListener) {
        //创建谷歌帮助类
        mHelper = new IabHelper(context, publicKey);
        mHelper.enableDebugLogging(true);
        /**
         * 初始化和连接谷歌服务
         */
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    mListener.onGoogleInitFailed(result.getMessage());
                } else {
                    mListener.onGoogleInitSuccess("init Success");
                    getLTOrderID(LTAppID, LTAppKey, packageId, gid, params);
                }
            }
        });
    }

    /**
     * 查询是否有未消费的商品
     *
     * @param context     上下文
     * @param requestCode 请求码
     * @param goodsList   商品集合
     * @param productID   内购产品唯一id, 填写你自己添加的内购商品id
     * @param mListener   回调
     */
    public static void checkUnConsume(final Context context, final int requestCode, List<String> goodsList,
                                      final String productID, final OnGooglePlayResultListener mListener) {
        try {
            List<String> subSku = new ArrayList<>();
            mHelper.queryInventoryAsync(true, goodsList, subSku,
                    new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                            if (result != null) {
                                if (result.isSuccess() && inv.hasPurchase(productID)) {
                                    //消费, 并下一步, 这里Demo里面我没做提示,将购买了,但是没消费掉的商品直接消费掉, 正常应该
                                    //给用户一个提示,存在未完成的支付订单,是否完成支付
                                    consumeProduct(context, inv.getPurchase(productID),
                                            false, "Consumption success",
                                            "Consumption failed");
                                } else {
                                    getProduct((Activity) context, requestCode, productID, mListener);
                                }
                            }
                        }

                    });
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
    }

    /**
     * 消费掉商品
     *
     * @param purchase
     * @param needNext
     * @param tipmsg1
     * @param tipmsg2
     */
    private static void consumeProduct(final Context context, Purchase purchase, final boolean needNext,
                                       final String tipmsg1, final String tipmsg2) {
        try {
            mHelper.consumeAsync(purchase, new IabHelper.OnConsumeFinishedListener() {
                @Override
                public void onConsumeFinished(Purchase purchase, IabResult result) {
                    if (mHelper == null) {
                        return;
                    }
                    if (result.isSuccess()) {
                        if (!needNext) {
                            //处理中断的情况, 仅仅只是消费掉上一次未正常完成的商品
                            Toast.makeText(context, tipmsg1, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, tipmsg2, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
    }


    /**
     * 产品获取
     *
     * @param context      上下文
     * @param REQUEST_CODE 请求码
     * @param SKU          产品唯一id, 填写你自己添加的商品id
     * @param mListener    回调监听
     */
    private static void getProduct(final Activity context, int REQUEST_CODE,
                                   final String SKU, final OnGooglePlayResultListener mListener) {
        if (!TextUtils.isEmpty(payload)) {
            if (mHelper != null) {
                try {
                    mHelper.launchPurchaseFlow(context, SKU, REQUEST_CODE, new IabHelper.OnIabPurchaseFinishedListener() {
                        @Override
                        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                            if (result.isFailure()) {
                                Toast.makeText(context, "Purchase Failed", Toast.LENGTH_SHORT).show();
                                mListener.onPlayError(result.getMessage());
                                return;
                            }
                            mListener.onPlaySuccess("Purchase successful");
                            if (purchase.getSku().equals(SKU)) {
                                //购买成功，调用消耗
                                consumeProduct(context, purchase, false, "Payment success",
                                        "Payment Failed");
                            }
                        }
                    }, payload);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }
            }
        } else {
            mListener.onPlayError("Order creation failed");
            Toast.makeText(context, "Order creation failed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 释放资源
     */
    public static void release() {
        /**
         * 释放掉资源
         */
        if (mHelper != null) {
            try {
                mHelper.dispose();
            } catch (IabHelper.IabAsyncInProgressException e) {
                e.printStackTrace();
            }
        }
        mHelper = null;
    }


    public static void onActivityResult(int requestCode, Intent data, int selfRequestCode,
                                        final String LTAppID, final String LTAppKey, OnGoogleResultListener
                                                mListener) {
        /**
         * 将回调交给帮助类来处理, 否则会出现支付正在进行的错误
         */
        if (mHelper != null) {
            if (requestCode == selfRequestCode) {
                int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
                //订单信息
                String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
                String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
                if (!TextUtils.isEmpty(purchaseData)) {
                    GoogleModel googleModel = new Gson().fromJson(purchaseData, GoogleModel.class);
                    Log.e(TAG, googleModel.getPurchaseToken());
                    Map<String, Object> params = new WeakHashMap<>();
                    params.put("purchase_token", googleModel.getPurchaseToken());
                    params.put("lt_order_id", payload);
                    uploadToServer(LTAppID, LTAppKey, params, mListener);
                }
            }
        }

    }

    /**
     * 获取乐推订单ID
     *
     * @param LTAppID   appID
     * @param LTAppKey  appKey
     * @param packageId 应用包名
     * @param gid       服务器配置商品的ID
     * @param params    集合
     */
    private static void getLTOrderID(String LTAppID, String LTAppKey, String gid,
                                     String packageId, Map<String, Object> params) {
        Map<String, Object> map = new WeakHashMap<>();
        map.put("package_id", packageId);
        map.put("gid", gid);
        map.put("custom", params);
        LoginBackManager.createOrder(LTAppID,
                LTAppKey, map, new OnCreateOrderListener() {
                    @Override
                    public void onOrderSuccess(String result) {
                        if (!TextUtils.isEmpty(result)) {
                            payload = result;
                        } else {
                            Log.e(TAG, "ltOrderID is null");
                        }
                    }

                    @Override
                    public void onOrderFailed(Throwable ex) {
                        Log.e(TAG, ex.getMessage());
                    }

                    @Override
                    public void onOrderError(String error) {
                        Log.e(TAG, error);
                    }
                });
    }

    private static void uploadToServer(
            final String LTAppID,
            final String LTAppKey,
            Map<String, Object> params,
            final OnGoogleResultListener mListener) {
        LoginBackManager.googlePlay(
                LTAppID, LTAppKey, params
                , new OnGooglePlayResultListener() {
                    @Override
                    public void onPlaySuccess(String result) {
                        mListener.onResultSuccess(result);
                    }

                    @Override
                    public void onPlayFailed(Throwable ex) {
                        mListener.onResultError(ex);
                    }

                    @Override
                    public void onPlayComplete() {

                    }

                    @Override
                    public void onPlayError(String result) {
                        mListener.onResultFailed(result);
                    }
                });
    }

}
