package com.vixiar.indicor.Graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.vixiar.indicor.R;

public class TestPressureGraph extends View
{
    private TextPaint textPaint;
    private double m_nBallPressure = (double)0.0;

    int paddingLeft;
    int paddingTop;
    int paddingRight;
    int paddingBottom;

    int contentWidth;
    int contentHeight;

    int barWidth;
    int barWidthMax;
    int barLeftStart;
    int barHeight;
    float heightPerMMHg;
    int barLeftPosition;
    int topSegmentHeight;
    int middleSegmentHeight;
    int bottomSegmentHeight;
    int gapBetweenBars;
    int topSegmentTop;
    int topSegmentBottom;
    int middleSegmentTop;
    int middleSegmentBottom;
    int bottomSegmentTop;
    int bottomSegmentBottom;
    float ballRadius;
    float ballStroke;
    float ballInset;
    int pressureLabelSize;

    public TestPressureGraph(Context context)
    {
        super(context);
        init(null, 0);
    }

    public TestPressureGraph(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(attrs, 0);
    }

    public TestPressureGraph(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle)
    {
    }

    public final int ACTIVE = 1;
    public final int INACTIVE = 0;
    private int m_graphMode = INACTIVE;

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        CalculateMetrics();
        DrawBackground(canvas);
        DrawLabels(canvas);
        if (m_graphMode == ACTIVE)
        {
            DrawBall(canvas);
        }
    }

    public void setBallPressure(double pressure) {
        m_nBallPressure = pressure;
        invalidate();
    }

    public void CalculateMetrics()
    {
        paddingLeft = getPaddingLeft();
        paddingTop = getPaddingTop();
        paddingRight = getPaddingRight();
        paddingBottom = getPaddingBottom();

        contentWidth = getWidth() - paddingLeft - paddingRight;
        contentHeight = getHeight() - paddingTop - paddingBottom;

        barWidth = (int)((float)contentWidth * (float)0.35);
        barWidthMax = contentHeight / 4;

        if (barWidth > barWidthMax)
        {
           barWidth = barWidthMax;
        }
        // start the bar 25% in from the left of the usable space
        // this is to make room for the labels
        barLeftStart = (int)((float)contentWidth * (float)0.35) + paddingLeft;

        // subtract the width from the total space to allow for a half-circle at the top and bottom
        barHeight = contentHeight - barWidth;

        // figure out how many pixels per mm pressure
        heightPerMMHg = barHeight / 45;

        barLeftPosition = barLeftStart + paddingLeft;
        topSegmentHeight = (int)(heightPerMMHg * (float)15.0);
        middleSegmentHeight = (int)(heightPerMMHg * (float)14.0);
        bottomSegmentHeight = (int)(heightPerMMHg * (float)16.0);
        gapBetweenBars = barWidth / 14;

        topSegmentTop = paddingTop + (barWidth / 2);
        topSegmentBottom = topSegmentTop + topSegmentHeight - (gapBetweenBars / 2);

        middleSegmentTop = topSegmentBottom + gapBetweenBars;
        middleSegmentBottom = middleSegmentTop + middleSegmentHeight - (gapBetweenBars / 2);

        bottomSegmentTop = middleSegmentBottom + gapBetweenBars;
        bottomSegmentBottom = bottomSegmentTop + bottomSegmentHeight;

        // ball stuff
        ballInset = ((float)barWidth / (float)10.0);
        ballRadius = (int)((float)barWidth / (float)2.0) - ballInset;
        ballStroke = barWidth / (float)9.0;

        // pressure label size
        pressureLabelSize = barWidth / 8;
    }

    public void DrawBackground(Canvas canvas)
    {
        Paint topAndBottomPaint = new Paint(0);
        if (m_graphMode == INACTIVE)
        {
            topAndBottomPaint.setColor(ContextCompat.getColor(this.getContext(), R.color.colorGraphTopAndBottomInactive));
        }
        else
        {
            topAndBottomPaint.setColor(ContextCompat.getColor(this.getContext(), R.color.colorGraphTopAndBottomActive));
        }
        topAndBottomPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        topAndBottomPaint.setStyle(Paint.Style.FILL);

        Paint centerPaint = new Paint(0);
        if (m_graphMode == INACTIVE)
        {
            centerPaint.setColor(ContextCompat.getColor(this.getContext(), R.color.colorGraphCenterInactive));
        }
        else
        {
            centerPaint.setColor(ContextCompat.getColor(this.getContext(), R.color.colorGraphCenterActive));
        }
        centerPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        Paint targetLinePaint = new Paint(0);
        targetLinePaint.setColor(ContextCompat.getColor(this.getContext(), R.color.colorTargetLine));
        targetLinePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        targetLinePaint.setStrokeWidth(2);

        // draw the top rect
        canvas.drawRect(barLeftPosition, topSegmentTop, barWidth+barLeftPosition, topSegmentBottom, topAndBottomPaint);

        // draw the middle rect
        canvas.drawRect(barLeftPosition, middleSegmentTop, barWidth+barLeftPosition, middleSegmentBottom, centerPaint);

        // draw the bottom rect
        canvas.drawRect(barLeftPosition, bottomSegmentTop, barWidth+barLeftPosition, bottomSegmentBottom, topAndBottomPaint);

        // draw the top circle
        canvas.drawCircle(barLeftPosition + (barWidth / 2), topSegmentTop, barWidth / 2, topAndBottomPaint);

        // draw the bottom circle
        canvas.drawCircle(barLeftPosition + (barWidth / 2), bottomSegmentBottom, barWidth / 2, topAndBottomPaint);

        // draw a line across the center so you know the target pressure
        canvas.drawLine(barLeftPosition, middleSegmentTop + ((middleSegmentBottom - middleSegmentTop) / 2 ),
                barLeftPosition + barWidth, middleSegmentTop + ((middleSegmentBottom - middleSegmentTop) / 2 ), targetLinePaint);
    }

    public void DrawLabels(Canvas canvas)
    {
        Typeface tf = ResourcesCompat.getFont(getContext().getApplicationContext(), R.font.roboto_light_family);

        textPaint = new TextPaint();
        textPaint.setTypeface(tf);
        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(pressureLabelSize);
        canvas.drawText("0 mmHg", barLeftPosition - 5, bottomSegmentBottom, textPaint);
        canvas.drawText("16 mmHg", barLeftPosition - 5, bottomSegmentTop, textPaint);
        canvas.drawText("30 mmHg", barLeftPosition - 5, middleSegmentTop, textPaint);
        canvas.drawText("45 mmHg", barLeftPosition - 5, topSegmentTop, textPaint);
    }

    public void DrawBall(Canvas canvas)
    {
        Paint shadowBallPaint = new Paint(0);
        shadowBallPaint.setColor(Color.BLACK);
        shadowBallPaint.setStyle(Paint.Style.STROKE);
        shadowBallPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        shadowBallPaint.setAlpha(25);
        shadowBallPaint.setStrokeWidth(ballStroke);
        canvas.drawCircle(barLeftPosition + (barWidth / 2) + (ballStroke / 2), bottomSegmentBottom - (float)(m_nBallPressure * heightPerMMHg) + ballStroke, ballRadius, shadowBallPaint) ;

        Paint whiteBallPaint = new Paint(0);
        whiteBallPaint.setColor(Color.WHITE);
        whiteBallPaint.setStyle(Paint.Style.STROKE);
        whiteBallPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        whiteBallPaint.setStrokeWidth(ballStroke);
        canvas.drawCircle(barLeftPosition + (barWidth / 2), bottomSegmentBottom - (float)(m_nBallPressure * heightPerMMHg), ballRadius, whiteBallPaint) ;

        // draw the slight white tint in the center of the ball
        whiteBallPaint.setStyle(Paint.Style.FILL);
        whiteBallPaint.setAlpha(80);
        canvas.drawCircle(barLeftPosition + (barWidth / 2), bottomSegmentBottom - (float)(m_nBallPressure * heightPerMMHg), ballRadius, whiteBallPaint) ;

        // draw the little lines pointing in
        whiteBallPaint.setAlpha(255);
        whiteBallPaint.setStrokeWidth(ballStroke / 4);
        canvas.drawLine(barLeftPosition + (barWidth / 2) - ballRadius, bottomSegmentBottom - (float)(m_nBallPressure * heightPerMMHg) , barLeftPosition + (barWidth / 2) - (ballRadius / 3), bottomSegmentBottom - (float)(m_nBallPressure * heightPerMMHg), whiteBallPaint);
        canvas.drawLine(barLeftPosition + (barWidth / 2) + ballRadius, bottomSegmentBottom - (float)(m_nBallPressure * heightPerMMHg) , barLeftPosition + (barWidth / 2) + (ballRadius / 3), bottomSegmentBottom - (float)(m_nBallPressure * heightPerMMHg), whiteBallPaint);
    }

    public void SetGraphActiveMode(Integer graphMode)
    {
        m_graphMode = graphMode;
        invalidate();
    }
}
