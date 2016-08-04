package ru.mail.park.chat.loaders.images;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import ru.mail.park.chat.api.BlurBuilder;

/**
 * Created by Михаил on 22.05.2016.
 */
public class ImageFetchTask extends AsyncTask<Void, Void, Bitmap> {
    private IImageSettable imageView;
    private URL url;
    private ImageDownloadManager.Size size;
    private ImageDownloadManager manager;

    public ImageFetchTask(IImageSettable imageView, ImageDownloadManager.Size size,
                          ImageDownloadManager manager, URL url) {
        this.imageView = imageView;
        this.url = url;
        this.size = size;
        this.manager = manager;
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        try {
            Log.v(ImageFetchTask.class.getSimpleName(), "Fetching an image " + url.toString());
            final ImageDownloadManager.Size[] sizes = ImageDownloadManager.Size.values();

            Bitmap bm;
            bm = manager.getBitmapFromMemoryCache(url, size);
            if (bm == null) {
                bm = manager.getBitmapFromDiskCache(url, size);
                if (bm != null) {
                    Log.v(ImageFetchTask.class.getSimpleName(), "Fetching an image from disk");
                    manager.addBitmapToMemCache(url, bm, size);
                }
            } else {
                Log.v(ImageFetchTask.class.getSimpleName(), "Fetching an image from cache");
            }
            if (bm != null) {
                return bm;
            } else {
                Log.v(ImageFetchTask.class.getSimpleName(), "Fetching an image from web");
                try {
                    InputStream in = url.openStream();

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    bm = BitmapFactory.decodeStream(in, null, options);

                    Bitmap returnedBitmap = null;
                    if (bm != null) {
                        for (ImageDownloadManager.Size iterationSize : sizes) {
                            Integer resize = iterationSize.toInteger(manager);
                            Bitmap scaled;
                            if (resize != null) {
                                Resources r = manager.getResources();
                                float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, resize, r.getDisplayMetrics());
                                scaled = scaleDown(bm, px);
                                if (iterationSize == ImageDownloadManager.Size.HEADER_BACKGROUND) {
                                    scaled = BlurBuilder.blur(manager, scaled);
                                }
                            } else {
                                scaled = bm;
                            }
                            if (iterationSize == size) {
                                returnedBitmap = scaled;
                                manager.addBitmapToMemCache(url, scaled, iterationSize);
                            }
                            manager.addBitmapToDiskCache(url, scaled, iterationSize);
                        }
                    }

                    return returnedBitmap;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null) {
            imageView.setImage(bitmap);
        }
        manager.remove(imageView);
    }

    private static Bitmap scaleDown(Bitmap realImage, float maxImageSizePx) {
        float ratio = Math.min(
                (float) maxImageSizePx / realImage.getWidth(),
                (float) maxImageSizePx / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        return getResizedBitmap(realImage, height, width);
    }

    public URL getUrl() {
        return url;
    }

    public ImageDownloadManager.Size getSize() {
        return size;
    }

    private static Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth)
    {
        int width = bm.getWidth();
        int height = bm.getHeight();
        if (width == newWidth && height == newHeight) {
            return bm;
        }
        Log.i("width", String.valueOf(width));
        Log.i("height", String.valueOf(height));


        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        float middleX = newWidth / 2.0f;
        float middleY = newHeight / 2.0f;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.setScale(scaleWidth, scaleHeight, middleX, middleY);
        // recreate the new Bitmap
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(matrix);
        canvas.drawBitmap(bm, middleX - bm.getWidth() / 2, middleY - bm.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));
        return scaledBitmap;
    }
}

