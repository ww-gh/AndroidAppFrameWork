package com.zenglb.baselib.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;
import com.zenglb.commonlib.R;

import butterknife.ButterKnife;

/**
 * 基类就只做基类的事情,不要把业务层面的代码写到这里来 ！！
 *
 * FBI WARMING,不要为了方便，只有某几个Activity 才会用的（定位，Wi-Fi 数据收集啊，写在Base里面，那还抽象什么）
 *
 * 1.toolbar 的封装
 * 2.页面之间的跳转
 * 3.注意WebViewActivity 开启了多进程！
 *
 * @author zenglb 20170301
 */
public abstract class BaseActivity extends RxAppCompatActivity implements View.OnClickListener{
    private static final String TAG = BaseActivity.class.getSimpleName();
    private Toolbar mToolbar;
    public Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);

        setContentView(setLayoutId());


        mContext=BaseActivity.this;
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
        }
        initViews();
    }




    protected abstract int setLayoutId();
    protected abstract void initViews();

    public void onClick(View view){}  //不是必须的

    /*
	 * Activity的跳转
	 */
    public final void startActivity(Class<?> cla) {
        Intent intent = new Intent();
        intent.setClass(this, cla);
        startActivity(intent);
    }

//    /*
//     * Activity的跳转-带参数
//     */
//    public void setIntentClass(Class<?> cla, Object obj) {
//        Intent intent = new Intent();
//        intent.setClass(this, cla);
//        intent.putExtra(INTENTTAG, (Serializable) obj);
//        startActivity(intent);
//        overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
//    }

    /**
     * Activity -> webview Activity的跳转-带参数
     *
     * @param link
     */

    public final  void goWebView(String link) {
        Intent intent = new Intent();
        intent.setAction(BaseWebViewActivity.WEB_ACTION);
        intent.addCategory(BaseWebViewActivity.WEB_CATEGORY);
        intent.putExtra(BaseWebViewActivity.URL, link);
        startActivity(intent);
    }


    /**
     * Get toolbar
     *
     * @return support.v7.widget.Toolbar.
     */
    public final Toolbar getToolbar() {
        return (Toolbar) findViewById(R.id.toolbar);
    }


    /**
     * 设置头部标题
     *
     * @param title
     */
    public void setToolBarTitle(CharSequence title) {
        getToolbar().setTitle(title);
        setSupportActionBar(getToolbar());
    }

    /**
     * 版本号小于21的后退按钮图片
     */
    private void showBack() {
        getToolbar().setNavigationIcon(R.drawable.ic_back_copy);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed(); //返回事件
            }
        });
    }

    /**
     * 是否显示后退按钮,默认显示,可在子类重写该方法.
     *
     * @return
     */
    protected boolean isShowBacking() {
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (null != getToolbar() && isShowBacking()) {
            showBack();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
