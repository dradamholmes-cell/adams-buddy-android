package com.spartaneo.adamsbuddy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

final class WatchFaceArt {
    private WatchFaceArt() { }

    static Bitmap create(Context context) {
        final int size = 512;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setShader(new RadialGradient(256, 220, 330,
                new int[]{Color.rgb(25, 71, 84), Color.rgb(6, 21, 34)},
                null, Shader.TileMode.CLAMP));
        canvas.drawColor(Color.rgb(6, 21, 34));
        canvas.drawCircle(256, 256, 250, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        paint.setColor(Color.rgb(130, 222, 189));
        canvas.drawCircle(256, 256, 239, paint);
        paint.setStrokeWidth(2);
        paint.setColor(Color.rgb(65, 137, 154));
        canvas.drawCircle(256, 256, 217, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.rgb(244, 196, 119));
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(34);
        canvas.drawText("BUDDY", 256, 103, paint);

        // Leave the middle open for the clock placed by the watch's Custom-face editor.
        paint.setColor(Color.rgb(134, 201, 210));
        paint.setTextSize(19);
        canvas.drawText("YOUR POCKET HQ", 256, 135, paint);
        paint.setColor(Color.rgb(128, 214, 190));
        canvas.drawRoundRect(new RectF(130, 170, 382, 173), 2, 2, paint);

        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.buddy_icon);
        if (logo != null) {
            Paint pixel = new Paint();
            pixel.setFilterBitmap(false);
            canvas.drawBitmap(logo, new Rect(0, 0, logo.getWidth(), logo.getHeight()),
                    new Rect(200, 336, 312, 448), pixel);
            logo.recycle();
        }
        return bitmap;
    }

    static Bitmap createArcade() {
        Bitmap bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawColor(Color.rgb(7, 18, 28));
        paint.setColor(Color.rgb(33, 61, 73));
        canvas.drawCircle(256, 256, 246, paint);
        paint.setColor(Color.rgb(118, 205, 182));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(7);
        canvas.drawCircle(256, 256, 239, paint);
        paint.setStyle(Paint.Style.FILL);

        int[] shades = {
                Color.rgb(33, 80, 94), Color.rgb(44, 108, 118),
                Color.rgb(61, 135, 139), Color.rgb(88, 159, 151)
        };
        for (int i = 0; i < shades.length; i++) {
            int inset = 85 + i * 34;
            paint.setColor(shades[i]);
            canvas.drawRect(inset, 167 + i * 25, 512 - inset, 420 - i * 23, paint);
            paint.setColor(Color.rgb(8, 24, 35));
            canvas.drawRect(inset + 10, 177 + i * 25, 502 - inset,
                    410 - i * 23, paint);
        }

        paint.setColor(Color.rgb(238, 100, 91));
        canvas.drawCircle(256, 348, 35, paint);
        paint.setColor(Color.rgb(17, 25, 32));
        canvas.drawCircle(243, 343, 5, paint);
        canvas.drawCircle(269, 343, 5, paint);
        paint.setColor(Color.rgb(245, 197, 116));
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(28);
        canvas.drawText("BUDDY BLASTER", 256, 115, paint);
        paint.setTextSize(17);
        paint.setColor(Color.rgb(154, 223, 207));
        canvas.drawText("ARCADE WATCH FACE", 256, 143, paint);
        // The companion app places the actual clock over this static background.
        return bitmap;
    }
}
