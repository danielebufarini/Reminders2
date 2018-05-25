package com.danielebufarini.reminders2.ui;

import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

    private ImageView image;

    public DownloadImageTask(ImageView image) {

        this.image = image;
    }

    protected Bitmap doInBackground(String... urls) {

        String urldisplay = urls[0];
        Bitmap icon = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            icon = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
        return icon;
    }

    protected void onPostExecute(Bitmap result) {

        image.setImageBitmap(result);
    }
}
