package com.example.fittrackerpro.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Day 8B (Content Providers Syllabus Topic):
 * Reads a content:// URI returned by the Android Photo Picker via ContentResolver.openInputStream(uri),
 * center-crops and scales the image to 512x512, and saves it into private internal app storage
 * (context.getFilesDir()) so no external storage permission is required.
 */
public class ProfilePhotoHelper {

    private ProfilePhotoHelper() {
    }

    public static File getAvatarFile(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user != null) ? user.getUid() : "default_user";
        return new File(context.getFilesDir(), "avatar_" + uid + ".jpg");
    }

    public static boolean copyUriToInternalStorage(Context context, Uri contentUri) {
        if (contentUri == null) {
            return false;
        }
        try (InputStream in = context.getContentResolver().openInputStream(contentUri)) {
            if (in == null) {
                return false;
            }
            Bitmap decoded = BitmapFactory.decodeStream(in);
            if (decoded == null) {
                return false;
            }

            // Center-crop to a square and scale to 512x512 so memory stays small
            int w = decoded.getWidth();
            int h = decoded.getHeight();
            int side = Math.min(w, h);
            int x = (w - side) / 2;
            int y = (h - side) / 2;

            Bitmap square = Bitmap.createBitmap(decoded, x, y, side, side);
            Bitmap scaled = Bitmap.createScaledBitmap(square, 512, 512, true);

            File dest = getAvatarFile(context);
            try (FileOutputStream out = new FileOutputStream(dest)) {
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out);
                out.flush();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Bitmap loadAvatarBitmap(Context context) {
        File file = getAvatarFile(context);
        if (!file.exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }
}
