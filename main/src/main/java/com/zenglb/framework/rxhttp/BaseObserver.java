package com.zenglb.framework.rxhttp;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.CallSuper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.zenglb.baselib.utils.TextUtils;
import com.zenglb.framework.mvp_oauth.Oauth_MVP_Activity;
import com.zenglb.framework.retrofit.core.HttpResponse;
import com.zenglb.framework.retrofit.core.HttpUiTips;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import retrofit2.HttpException;

/**
 * Base Observer 的封装处理,对Rxjava 不熟悉，暂时先这样吧。实际的使用还不是很明白
 * <p>
 * 注意内存泄漏：https://github.com/trello/RxLifecycle/tree/2.x
 * <p>
 * Created by zenglb on 2017/4/14.
 */
public abstract class BaseObserver<T> implements Observer<HttpResponse<T>> {
    private final String TAG = BaseObserver.class.getSimpleName();
    public final static String Thread_Main="main";

    private final int RESPONSE_CODE_OK = 0;       //自定义的业务逻辑，成功返回积极数据
    private final int RESPONSE_FATAL_EOR = -1;  //返回数据失败,严重的错误

    private Context mContext;
    private static Gson gson = new Gson();

    private int errorCode = -1111;
    private String errorMsg = "未知的错误！";


    private Disposable disposable;

    /**
     * 根据具体的Api 业务逻辑去重写 onSuccess 方法！Error 是选择重写，but 必须Super ！
     *
     * @param t
     */
    public abstract void onSuccess(T t);


    /**
     * @param mCtx
     */
    public BaseObserver(Context mCtx) {
        this.mContext = mCtx;
        HttpUiTips.showDialog(mContext, true, null);
    }

    /**
     * @param mCtx
     * @param showProgress 默认需要显示进程，不要的话请传 false
     */
    public BaseObserver(Context mCtx, boolean showProgress) {
        this.mContext = mCtx;
        if (showProgress) {
            HttpUiTips.showDialog(mContext, true, null);
        }
    }

    @Override
    public final void onSubscribe(Disposable d) {
//        d.dispose(); //disable() 将会使得订阅关系断开，不再接收到事件,使用Rxlife 后不断开也行吧，开销是什么？
        disposable=d;
    }

    @Override
    public final void onNext(HttpResponse<T> response) {
        HttpUiTips.dismissDialog(mContext);

        if(!disposable.isDisposed()){
            disposable.dispose();
        }

        if (response.getCode() == RESPONSE_CODE_OK) {
            onSuccess(response.getResult());
        } else {
            onFailure(response.getCode(), response.getError());
        }

    }

    @Override
    public final void onError(Throwable t) {
        HttpUiTips.dismissDialog(mContext);
        if (t instanceof HttpException) {
            HttpException httpException = (HttpException) t;
            errorCode = httpException.code();
            errorMsg = httpException.getMessage();
            getErrorMsg(httpException);
        } else if (t instanceof SocketTimeoutException) {  //VPN open
            errorCode = RESPONSE_FATAL_EOR;
            errorMsg = "服务器响应超时";
        } else if (t instanceof ConnectException) {
            errorCode = RESPONSE_FATAL_EOR;
            errorMsg = "网络连接异常，请检查网络";
        } else if (t instanceof RuntimeException) {
            errorCode = RESPONSE_FATAL_EOR;
            errorMsg = "运行时错误";
        } else if (t instanceof UnknownHostException) {
            errorCode = RESPONSE_FATAL_EOR;
            errorMsg = "无法解析主机，请检查网络连接";
        } else if (t instanceof UnknownServiceException) {
            errorCode = RESPONSE_FATAL_EOR;
            errorMsg = "未知的服务器错误";
        } else if (t instanceof IOException) {  //飞行模式等
            errorCode = RESPONSE_FATAL_EOR;
            errorMsg = "没有网络，请检查网络连接";
        }
        onFailure(errorCode, errorMsg);  //
    }

    /**
     * 简单的把Dialog 处理掉
     */
    @Override
    public final void onComplete() {


//        HttpUiTips.dismissDialog(mContext);
    }

    /**
     * Default error dispose!
     * 一般的就是 AlertDialog 或 SnackBar
     *
     * @param code
     * @param message
     */
    @CallSuper  //if overwrite,you should let it run.
    public void onFailure(int code, String message) {
        if (code == RESPONSE_FATAL_EOR && mContext != null) {
            HttpUiTips.alertTip(mContext, message, code);
        } else {
            disposeEorCode(message, code);
        }
    }

    /**
     * 对通用问题的统一拦截处理
     *
     * @param code
     */
    private final void disposeEorCode(String message, int code) {
        switch (code) {
            case 101:
            case 112:
            case 123:
            case 401:
                //退回到登录页面
                if (mContext != null) {  //Context 可以使Activity BroadCast Service !
                    Intent intent = new Intent();
                    intent.setClass(mContext, Oauth_MVP_Activity.class);
                    mContext.startActivity(intent);
                }
                break;
        }

        if (mContext != null&& Thread.currentThread().getName().toString().equals(Thread_Main)) {
            Toast.makeText(mContext, message + "   code=" + code, Toast.LENGTH_SHORT).show();
        }

    }


    /**
     * 获取详细的错误的信息 errorCode,errorMsg
     * <p>
     * 以登录的时候的Grant_type 故意写错为例子,这个时候的http 应该是直接的返回401=httpException.code()
     * 但是是怎么导致401的？我们的服务器会在respose.errorBody 中的content 中说明
     */
    private final void getErrorMsg(HttpException httpException) {
        String errorBodyStr = "";
        try {      //我们的项目需要的UniCode转码，不是必须要的！
            errorBodyStr = TextUtils.convertUnicode(httpException.response().errorBody().string());
        } catch (IOException ioe) {
            Log.e("errorBodyStr ioe:", ioe.toString());
        }
        try {
            HttpResponse errorResponse = gson.fromJson(errorBodyStr, HttpResponse.class);
            if (null != errorResponse) {
                errorCode = errorResponse.getCode();
                errorMsg = errorResponse.getError();
            }
        } catch (Exception jsonException) {
            jsonException.printStackTrace();
        }
    }

}
