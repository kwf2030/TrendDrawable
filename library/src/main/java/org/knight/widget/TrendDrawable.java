package org.knight.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class TrendDrawable extends Drawable {
    private static final String PROPERTY = "index";

    private static final int X_AXIS_TOP = 0xFFFFFFFF;
    private static final int X_AXIS_BOTTOM = 0xFFFFFFFE;
    private static final int X_AXIS_HALF = 0xFFFFFFFD;

    private ArrayList<Float> data, factors;
    private float[] centers, crossovers;

    private Paint paint;
    private Animator animator;

    private Colors colors;
    private Sizes sizes;

    private float maxValue, minValue, zLine;
    private int hAreas, vAreas;

    private boolean ready;
    private int animIndex;

    public TrendDrawable(ArrayList<Float> data) {
        if (data == null || data.size() == 0) {
            throw new NullPointerException("data is null");
        }
        init(data);
    }

    private void init(ArrayList<Float> data) {
        this.data = data;
        if (factors == null) {
            factors = new ArrayList<Float>();
        }
        factors.clear();
        centers = new float[data.size() * 2];
        crossovers = new float[data.size() * 4];
        if (paint == null) {
            paint = new Paint();
        }
        paint.reset();
        paint.setAntiAlias(true);
        if (colors == null) {
            colors = new Colors();
        }
        if (sizes == null) {
            sizes = new Sizes();
        }
        maxValue = Collections.max(data);
        minValue = Collections.min(data);
        hAreas = (data.size() - 1) == 0 ? 1 : (data.size() - 1);
        float absMax = Math.max(Math.abs(maxValue), Math.abs(minValue));
        if (maxValue == minValue) {
            vAreas = (int) Math.ceil(absMax) * 2;
            zLine = X_AXIS_HALF;
        } else if (maxValue > 0 && minValue > 0) {
            vAreas = (int) Math.ceil(absMax);
            zLine = X_AXIS_BOTTOM;
        } else if (maxValue < 0 && minValue < 0) {
            vAreas = (int) Math.ceil(absMax);
            zLine = X_AXIS_TOP;
        } else {
            float v = maxValue - minValue;
            vAreas = (int) Math.ceil(v);
            if (maxValue + minValue == 0 || v == 0) {
                zLine = X_AXIS_HALF;
            } else {
                zLine = maxValue / v;
            }
        }
        ready = false;
        animIndex = 0;
    }

    private void prepare(float l, float t, float b, float sw, float sh) {
        Arrays.fill(centers, 0);
        Arrays.fill(crossovers, 0);

        if (zLine == X_AXIS_TOP) {
            zLine = t;
        } else if (zLine == X_AXIS_BOTTOM) {
            zLine = b;
        } else if (zLine == X_AXIS_HALF) {
            zLine = (t + b) / 2;
        } else {
            zLine = t + (b - t) * zLine;
        }

        float r = sizes.cr;
        int len = data.size();
        for (int i = 0; i < len; i++) {
            float v = data.get(i);
            float x = l + i * sw, y;
            if (v >= 0) {
                y = zLine - v * sh;
            } else {
                y = zLine + Math.abs(v) * sh;
            }
            centers[i * 2] = x;
            centers[i * 2 + 1] = y;

            if (i > 0) {
                float[] p = calcCrossoverPoint(centers[(i - 1) * 2], centers[(i - 1) * 2 + 1], r, x, y, r);
                int pos = (i - 1) * 4 + 2;
                System.arraycopy(p, 0, crossovers, pos, 4);
            }
        }

        animator = ObjectAnimator.ofInt(this, PROPERTY, 0, data.size());
    }

    private void drawBoarder(Canvas canvas, float l, float t, float r, float b, float sw) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(colors.bl);
        paint.setStrokeWidth(sizes.bl);
        canvas.drawLine(l, t, l, b, paint);
        canvas.drawLine(r, t, r, b, paint);

        paint.setColor(colors.vsl);
        paint.setStrokeWidth(sizes.vsl);
        float y1 = t + sizes.o;
        float y2 = b - sizes.o;
        int s = hAreas;
        for (int i = 1; i < s; i++) {
            canvas.drawLine(l + i * sw, y1, l + i * sw, y2, paint);
        }
    }

    private void drawZeroAxis(Canvas canvas, float l, float r) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(colors.xa);
        paint.setStrokeWidth(sizes.xa);
        canvas.drawLine(l, zLine, r, zLine, paint);
    }

    private void drawTrend(Canvas canvas) {
        int len = factors.size();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                drawLine(canvas, i - 1);
            }
            drawPoint(canvas, centers[i * 2], centers[i * 2 + 1]);
        }
    }

    private void drawPoint(Canvas canvas, float x, float y) {
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(sizes.oc);
        float r = sizes.cr;
        if (y <= zLine) {
            paint.setColor(colors.pt);
        } else {
            paint.setColor(colors.nt);
        }
        canvas.drawCircle(x, y, r, paint);
        paint.setColor(colors.ic);
        canvas.drawCircle(x, y, r * sizes.ics, paint);
    }

    private void drawLine(Canvas canvas, int index) {
        int pos = index * 2;
        float x1 = centers[pos];
        float y1 = centers[pos + 1];
        float x2 = centers[pos + 2];
        float y2 = centers[pos + 3];
        float[] p = getCrossoverPoint(index);

        paint.setStrokeWidth(sizes.tl);
        float x = -1, y = zLine;
        if (y1 < y && y2 < y) {
            paint.setColor(colors.pt);
        } else if (y1 > y && y2 > y) {
            paint.setColor(colors.nt);
        } else {
            float k = (y2 - y1) / (x2 - x1);
            float c = y1 - x1 * k;
            x = (y - c) / k;
        }
        if (x == -1) {
            canvas.drawLine(p[0], p[1], p[2], p[3], paint);
        } else {
            if (y1 < y) {
                paint.setColor(colors.pt);
                canvas.drawLine(p[0], p[1], x, y, paint);
                paint.setColor(colors.nt);
                canvas.drawLine(x, y, p[2], p[3], paint);
            } else {
                paint.setColor(colors.nt);
                canvas.drawLine(p[0], p[1], x, y, paint);
                paint.setColor(colors.pt);
                canvas.drawLine(x, y, p[2], p[3], paint);
            }
        }
    }

    private void drawValue(Canvas canvas, float l, float t, float r, float b) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colors.t);
        paint.setTextSize(sizes.t);
        if (maxValue == minValue) {
            float y;
            if (maxValue >= 0) {
                y = t + 20;
            } else {
                y = b - 20;
            }
            canvas.drawText(maxValue > 0 ? "+" + maxValue : (maxValue < 0 ? "" + maxValue : "0"), (l + r) / 2, y, paint);
        } else {
            canvas.drawText(maxValue > 0 ? "+" + maxValue : (maxValue < 0 ? "" + maxValue : "0"), (l + r) / 2, t + 20, paint);
            canvas.drawText(minValue > 0 ? "+" + minValue : (minValue < 0 ? "" + minValue : "0"), (l + r) / 2, b - 20, paint);
        }
    }

    private float[] getCrossoverPoint(int index) {
        float[] ret = new float[4];
        System.arraycopy(crossovers, index * 4 + 2, ret, 0, 4);
        return ret;
    }

    private float[] calcCrossoverPoint(float cx1, float cy1, float r1, float cx2, float cy2, float r2) {
        if (cy1 == cy2) {
            return new float[]{cx1 + r1, cy1, cx2 - r2, cy2};
        }
        float x1, y1, x2, y2;
        float x = Math.abs(cx2 - cx1);
        float y = Math.abs(cy2 - cy1);
        float k = y / x;
        double rad = Math.atan(k);

        double oy1 = Math.sin(rad) * r1;
        double ox1 = Math.sqrt(r1 * r1 - oy1 * oy1);
        x1 = (float) (cx1 + ox1);
        if (cy1 > cy2) {
            y1 = (float) (cy1 - oy1);
        } else {
            y1 = (float) (cy1 + oy1);
        }

        double v = Math.hypot(x, y) - r2;
        double oy2 = Math.sin(rad) * v;
        double ox2 = Math.sqrt(v * v - oy2 * oy2);
        x2 = (float) (cx1 + ox2);
        if (cy1 > cy2) {
            y2 = (float) (cy1 - oy2);
        } else {
            y2 = (float) (cy1 + oy2);
        }

        return new float[]{x1, y1, x2, y2};
    }

    public TrendDrawable refresh(ArrayList<Float> data) {
        if (data == null || data.size() == 0) {
            throw new NullPointerException("data is null");
        }
        init(data);
        invalidateSelf();
        return this;
    }

    public void startAnimation() {
        if (animator != null) {
            if (animator.isStarted()) {
                animator.cancel();
            }
            animator.start();
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    startAnimation();
                }
            }, 100);
        }
    }

    public TrendDrawable setColors(Colors colors) {
        if (colors != null) {
            this.colors = colors;
        }
        return this;
    }

    public TrendDrawable setSizes(Sizes sizes) {
        if (sizes != null) {
            this.sizes = sizes;
        }
        return this;
    }

    public void setIndex(int index) {
        if (animIndex < data.size()) {
            factors.add(data.get(animIndex++));
            invalidateSelf();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect rect = getBounds();
        float l = rect.left + sizes.hm;
        float r = rect.right - sizes.hm;
        float t = rect.top + sizes.vm;
        float b = rect.bottom - sizes.vm;
        float t1 = t + sizes.cr + sizes.oc * 2;
        float b1 = b - sizes.cr - sizes.oc * 2;

        float sw = (r - l) / hAreas;
        float sh = (b1 - t1) / vAreas;

        if (!ready) {
            prepare(l, t1, b1, sw, sh);
            ready = true;
        }
        drawBoarder(canvas, l, t, r, b, sw);
        drawZeroAxis(canvas, l, r);
        drawTrend(canvas);
        //drawValue(canvas, l, t, r, b);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return 1;
    }

    public static class Colors {
        //正值颜色（走势线与外圆）PositiveTrend
        public int pt = Color.parseColor("#DC7066");

        //负值颜色（走势线与外圆）NegativeTrend
        public int nt = Color.parseColor("#72BC67");

        //零值颜色（外圆）ZeroCircle
        public int zc = Color.parseColor("#DC7066");

        //正值颜色（区块渐变色）PositiveArea
        public int pa = Color.parseColor("#865B6E");

        //负值颜色（区块渐变色）NegativeArea
        public int na = Color.parseColor("#5D7F4D");

        //文字颜色Text
        public int t = Color.parseColor("#6B8292");

        //边界线颜色BorderLine
        public int bl = Color.parseColor("#577287");

        //垂直分割线颜色VerticalSplitterLine
        public int vsl = Color.parseColor("#577287");

        //X轴颜色X_Axis
        public int xa = Color.parseColor("#577287");

        //内圆颜色InnerCircle
        public int ic = Color.parseColor("#FFFFFF");
    }

    public static class Sizes {
        //垂直边距VerticalMargin
        public int vm = 0;

        //水平边距HorizontalMargin
        public int hm = 20;

        //边界线和垂直分割线的偏移量Offset
        public int o = 10;

        //边界线画笔的粗细BorderLine
        public int bl = 2;

        //X轴画笔的粗细X_Axis
        public int xa = 2;

        //垂直分割线画笔的粗细VerticalSplitterLine
        public int vsl = 1;

        //走势线画笔的粗细TrendLine
        public int tl = 2;

        //文字大小Text
        public int t = 20;

        //外圆画笔的粗细OuterCircle
        public int oc = 1;

        //外圆半径CircleRadius
        public int cr = 8;

        //内圆的缩放比例InnerCircleScale
        public float ics = 0.6f;
    }
}
