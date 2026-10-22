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
}
