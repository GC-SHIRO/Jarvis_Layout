package com.example.jarvis_layout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class AudioVisualizerSphereView extends View {
    private Paint spherePaint;
    private float baseRadius;    // 基础半径
    private float currentRadius; // 当前半径
    private float centerX;
    private float centerY;

    public AudioVisualizerSphereView(Context context){
        super(context);
        init();
    }

    public AudioVisualizerSphereView(Context context, AttributeSet attrs){
        super(context, attrs);
        init();
    }

    public AudioVisualizerSphereView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
        init();
    }

    // 初始化画笔和初始半径
    private void init(){
        spherePaint = new Paint();
        spherePaint.setColor(Color.parseColor("#2a8a9d"));
        spherePaint.setStyle(Paint.Style.STROKE);
        spherePaint.setStrokeWidth(5f);
        spherePaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;

        // 根据视图大小动态调整基础半径
        baseRadius = Math.min(w, h) / 5f;
        currentRadius = baseRadius; // 初始化为基础半径
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        // 只绘制一个圆
        canvas.drawCircle(centerX, centerY, currentRadius, spherePaint);
    }

    // 设置音频振幅，动态调整半径
    public void setAmplitude(float amplitude){
        if (amplitude > 0) {
            // 根据振幅动态调整半径
            currentRadius = baseRadius * (1 + amplitude);
        } else {
            // 没有声音时，将半径设置为基础半径
            currentRadius = baseRadius;
        }
        invalidate(); // 触发重绘
    }
}
