package xps.panshen.com.popupcirclemenu;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;


public class PopupCircleView extends RelativeLayout implements Handler.Callback {
    private String TAG = getClass().getName();
    private Activity mContext;
    private Popup mPopup;
    private Rect mTriggerRect;
    private ViewGroup mDecorView;
    private LayoutParams mLayoutParams;
    private ArrayList<PopupButton> mButtons = new ArrayList<>();
    private ValueAnimator mAlphAnimator;
    private OnMenuEventListener mOnMenuEventListener;
    private OnButtonPreparedListener onButtonPreparedListener;

    private static boolean isshowing;
    private static final int ACTION_DOWN = 0;
    private static final int ACTION_UP = 1;

    public static final int UNDEFIEN = -1;
    public static final int RIGHT = 2;
    public static final int LEFT = 1;
    public int mOpenDirection = UNDEFIEN;

    private int mTriggerPixle = 25;

    private boolean mAbleToggle;

    private int mRadius;
    private int mAnimDuration = 250;
    private Handler mHandler;

    public void setmOnMenuEventListener(OnMenuEventListener mOnMenuEventListener) {
        this.mOnMenuEventListener = mOnMenuEventListener;
    }

    public void setOnButtonPreparedListener(OnButtonPreparedListener onButtonPreparedListener) {
        if (this.onButtonPreparedListener == null) {
            this.onButtonPreparedListener = onButtonPreparedListener;
        } else {
            resetbutton();
            onButtonPreparedListener.onPrepared(mButtons);
        }
    }

    public PopupCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = (Activity) context;
        mHandler = new Handler(Looper.getMainLooper(), this);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CircleMenu, 0, 0);
        initDefaultParam();

        mRadius = a.getDimensionPixelSize(R.styleable.CircleMenu_radius, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 100, getResources().getDisplayMetrics()));
        mAnimDuration = a.getInt(R.styleable.CircleMenu_anim_duration, 250);
        mOpenDirection = a.getInt(R.styleable.CircleMenu_open_direction, UNDEFIEN);

        a.recycle();
        init();

    }

    void initButtons() {
        if (!(getChildCount() >= 1)) {
            throw new RuntimeException("you mast add at least one childView");
        }

        View view = getChildAt(getChildCount() - 1);
        if (view instanceof PopupButton) {
            throw new RuntimeException("Don't place PopupButton on bottom of the PopupCircleView");
        }

        for (int i = 0; i < getChildCount() - 1; i++) {
            PopupButton pb = (PopupButton) getChildAt(i);
            pb.setmAnimDuration(mAnimDuration);
            mButtons.add(pb);
        }

        removeViews(0, getChildCount() - 1);
        mPopup.setbts(mButtons);

    }

    private Point getViewCenterPoint() {
        Rect r = new Rect();
        Point p = new Point();
        getGlobalVisibleRect(r);
        p.set(r.centerX(), r.centerY());
        return p;
    }

    private void initDefaultParam() {
        mRadius = getResources().getDimensionPixelSize(R.dimen.default_radius);
    }

    boolean juageCallback() {
        return mOnMenuEventListener != null &&
                mPopup.mSelectedIndex != -1 &&
                mPopup.mSelectedIndex != 0;
    }

    private void init() {
        mLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mDecorView = (ViewGroup) mContext.getWindow().getDecorView();

        mPopup = new Popup(mContext, mRadius);
        mPopup.setVisibility(INVISIBLE);
        mAlphAnimator = new ValueAnimator();
        mAlphAnimator.setFloatValues(0.0f, 1.0f);
        mAlphAnimator.setDuration(mAnimDuration);
        mAlphAnimator.setInterpolator(new LinearOutSlowInInterpolator());
        mAlphAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPopup.setShadowViewAlpha(Float.valueOf(animation.getAnimatedValue() + ""));
            }
        });
        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                initButtons();
                if (onButtonPreparedListener != null)
                    onButtonPreparedListener.onPrepared(mButtons);
            }
        });
    }

    void resetbutton() {
        for (PopupButton pb : mButtons) {
            pb.setChecked(false);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                int downX = (int) ev.getRawX();
                int downY = (int) ev.getRawY();

                if (isshowing) {
                    return false;
                } else {
                    mTriggerRect = new Rect(downX - mTriggerPixle, downY - mTriggerPixle, downX + mTriggerPixle, downY + mTriggerPixle);
                    getParent().requestDisallowInterceptTouchEvent(true);

                    mAbleToggle = true;
                    Message msg = Message.obtain();
                    msg.obj = ev;
                    msg.what = ACTION_DOWN;
                    if (mPopup.getVisibility() == INVISIBLE) {
                        mHandler.sendMessageDelayed(msg, mAnimDuration);
                    }

                    return true;
                }
            case MotionEvent.ACTION_MOVE:
                int mCurrentX = (int) ev.getRawX();
                int mCurrentY = (int) ev.getRawY();

                if (mTriggerRect.contains(mCurrentX, mCurrentY)) {
                    if (mPopup.getVisibility() == VISIBLE && isshowing) {
                        mPopup.dispatchTouchEvent(ev);
                    }
                } else {
                    if (mPopup.getVisibility() == INVISIBLE) {
                        mAbleToggle = false;
                        getParent().requestDisallowInterceptTouchEvent(false);
                    } else {
                        mPopup.dispatchTouchEvent(ev);
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
                dismiss(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
                dismiss(ev);
                break;
        }
        return false;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == ACTION_DOWN) {
            if (mAbleToggle && mPopup.getVisibility() == INVISIBLE && !isshowing) {

                /**
                 * 如果没有设置回调代表不关心勾选状态
                 * 主动清除勾选状态 防止View被复用时状态错乱
                 * */
                if (mOnMenuEventListener == null)
                    resetbutton();

                MotionEvent newEv = (MotionEvent) msg.obj;
                isshowing = true;
                mAlphAnimator.start();
                mPopup.setVisibility(VISIBLE);
                mDecorView.addView(mPopup, mLayoutParams);

                if (mOpenDirection != UNDEFIEN)
                    mPopup.resetCenter(getViewCenterPoint(), mOpenDirection);
                else
                    mPopup.resetCenter(new Point((int) newEv.getRawX(), (int) newEv.getRawY()), mOpenDirection);

                newEv.setAction(MotionEvent.ACTION_DOWN);
                mPopup.dispatchTouchEvent(newEv);
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        } else if (msg.what == ACTION_UP) {
            mPopup.setVisibility(INVISIBLE);
            mDecorView.removeView(mPopup);
            isshowing = false;
            mAbleToggle = false;
            getParent().requestDisallowInterceptTouchEvent(false);
            if (juageCallback()) {
                mOnMenuEventListener.onMenuToggle(mButtons.get(mPopup.getmSelectedIndex() - 1), mPopup.getmSelectedIndex());
            }
        }
        return false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public interface OnMenuEventListener {
        void onMenuToggle(PopupButton popupButton, int index);
    }

    public interface OnButtonPreparedListener {
        void onPrepared(ArrayList<PopupButton> bts);
    }

    private void dismiss(MotionEvent ev) {
        isshowing = false;
        if (mPopup.getVisibility() == VISIBLE) {
            mAlphAnimator.reverse();
            Message msg = Message.obtain();
            msg.obj = ev;
            msg.what = ACTION_UP;
            mHandler.sendMessageDelayed(msg, mAnimDuration);
            mPopup.dispatchTouchEvent(ev);
        } else {
            if (mPopup.getVisibility() == INVISIBLE && getChildAt(0) != null && mTriggerRect.contains((int) ev.getRawX(), (int) ev.getRawY()))
                getChildAt(getChildCount() - 1).performClick();
            mHandler.removeMessages(ACTION_DOWN);

        }
    }

}
