package com.fandy.handlerdemo;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.widget.Toast;

import java.lang.ref.WeakReference;

/**
 * handler相关知识的从新回顾
 */
public class MainActivity extends Activity {

    private Looper mMainLooper;
    private MessageQueue mQueue;
    private Looper mLooper;
    private Looper mLooper1;

    /**
     * 两种不同的创建handler的方式
     */


    /**
     * 正确的创建方式,是采用静态内部类,并且使用弱引用进行引用起来
     */

    private class MyHandler extends Handler {

        WeakReference<MainActivity> mReference;

        public MyHandler(MainActivity activity) {
            mReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity mainActivity = mReference.get();
            if (mainActivity != null) {
                //里边在进行应该有的操作
            }
        }
    }

    //非静态的匿名内部类,可能造成内存泄漏
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    /**
     * 可以避免内存泄漏
     * 官方也推荐使用这种方式进行编写
     */
    private Handler handler1 = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Toast.makeText(MainActivity.this, "避免内存泄漏的方式创建", Toast.LENGTH_SHORT).show();
            //如果不需要进一步处理，则返回True
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        threadLocalTest();
        mMainLooper = this.getMainLooper();
        handler1.sendEmptyMessage(0);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            mQueue = mMainLooper.getQueue();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                //一个线程只能开启一个Looper: java.lang.RuntimeException: Only one Looper may be created per thread
                //Looper.prepare();
                Looper.prepare();
                System.out.println("第一个线程中创建looper");
                Handler handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        System.out.println("被执行了");
                    }
                };
                mLooper = Looper.myLooper();
                handler.sendEmptyMessage(0);
                Looper.loop();
            }
        }).start();

        /**
         * 开启一个线程
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                System.out.println("第二个线程中创建looper");
                //Looper.getMainLooper() :可以在任意线程中获取当前主线程的Looper
                Looper mainLooper = Looper.getMainLooper();
                mLooper1 = Looper.myLooper();
                Looper.loop();
            }
        }).start();

    }

    /**
     * ThreadLocal 用来存储线程中的数据存储
     * jdk1.8之后,底层的实现采用了map
     */
    private void threadLocalTest() {
        /**
         * 相当于维护了三个数组
         */
        final ThreadLocal<Boolean> threadLocal = new ThreadLocal<>();
        threadLocal.set(true);
        System.out.println("主线程: " + threadLocal.get());//true
        new Thread(new Runnable() {
            @Override
            public void run() {
                threadLocal.set(false);
                System.out.println("线程1: " + threadLocal.get());//false
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("线程2: " + threadLocal.get());//null
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            /**
             *   证明,,不同线程拥有不同的消息队列
             *   多个线程拥有多个消息队列
             */
            MessageQueue queue = mLooper.getQueue();
            System.out.println(mLooper.getQueue() == mLooper1.getQueue());//false
        }
    }
}
