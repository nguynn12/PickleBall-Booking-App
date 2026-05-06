package com.example.pickleball.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper class for image operations
 * - Pick image from gallery/camera
 * - Compress images
 * - Upload to Firebase Storage
 */
public class ImageHelper {

    private static final String TAG = "ImageHelper";

    // ==================== IMAGE COMPRESSION ====================

    /**
     * Compress image from URI
     * @param context Application context
     * @param imageUri Image URI from picker
     * @return Compressed byte array
     */
    public static byte[] compressImage(Context context, Uri imageUri) {
        try {
            // Load bitmap from URI
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI");
                return null;
            }

            // Resize if too large
            Bitmap resizedBitmap = resizeBitmap(originalBitmap, Constants.MAX_IMAGE_DIMENSION);

            // Compress to JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, Constants.COMPRESSED_IMAGE_QUALITY, baos);
            byte[] data = baos.toByteArray();

            // Cleanup
            if (resizedBitmap != originalBitmap) {
                resizedBitmap.recycle();
            }
            originalBitmap.recycle();

            Log.d(TAG, "Image compressed: " + data.length + " bytes");
            return data;

        } catch (IOException e) {
            Log.e(TAG, "Error compressing image", e);
            return null;
        }
    }

    /**
     * Resize bitmap if dimensions exceed max
     */
    private static Bitmap resizeBitmap(Bitmap original, int maxDimension) {
        int width = original.getWidth();
        int height = original.getHeight();

        // No resize needed
        if (width <= maxDimension && height <= maxDimension) {
            return original;
        }

        // Calculate scale
        float scale;
        if (width > height) {
            scale = (float) maxDimension / width;
        } else {
            scale = (float) maxDimension / height;
        }

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    // ==================== FIREBASE STORAGE UPLOAD ====================

    /**
     * Upload avatar to Firebase Storage
     * @param userId User ID for file naming
     * @param imageData Compressed image bytes
     * @param callback Upload result callback
     */
    public static void uploadAvatar(String userId, byte[] imageData, UploadCallback callback) {
        if (imageData == null || imageData.length == 0) {
            callback.onFailure(new Exception("Image data is empty"));
            return;
        }

        // Check file size (5MB limit)
        if (imageData.length > Constants.MAX_IMAGE_SIZE_MB * 1024 * 1024) {
            callback.onFailure(new Exception("Image size exceeds 5MB limit"));
            return;
        }

        // Firebase Storage reference
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference avatarRef = storageRef.child(Constants.STORAGE_AVATARS + userId + ".jpg");

        // Upload
        UploadTask uploadTask = avatarRef.putBytes(imageData);
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress((int) progress);
            Log.d(TAG, "Upload progress: " + progress + "%");
        })
        .addOnSuccessListener(taskSnapshot -> {
            // Get download URL
            avatarRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d(TAG, "Upload successful: " + downloadUrl);
                callback.onSuccess(downloadUrl);
            }).addOnFailureListener(callback::onFailure);
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Upload failed", e);
            callback.onFailure(e);
        });
    }

    /**
     * Upload court image to Firebase Storage
     * @param courtId Court ID for file naming
     * @param imageData Compressed image bytes
     * @param callback Upload result callback
     */
    public static void uploadCourtImage(String courtId, byte[] imageData, UploadCallback callback) {
        if (imageData == null || imageData.length == 0) {
            callback.onFailure(new Exception("Image data is empty"));
            return;
        }

        // Firebase Storage reference
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String fileName = courtId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(Constants.STORAGE_COURT_IMAGES + fileName);

        // Upload
        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress((int) progress);
        })
        .addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                callback.onSuccess(uri.toString());
            }).addOnFailureListener(callback::onFailure);
        })
        .addOnFailureListener(callback::onFailure);
    }

    /**
     * Delete image from Firebase Storage
     * @param imageUrl Full download URL
     * @param callback Delete result callback
     */
    public static void deleteImage(String imageUrl, DeleteCallback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onFailure(new Exception("Image URL is empty"));
            return;
        }

        try {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference imageRef = storage.getReferenceFromUrl(imageUrl);

            imageRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Image deleted successfully");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete image", e);
                    callback.onFailure(e);
                });
        } catch (Exception e) {
            Log.e(TAG, "Invalid image URL", e);
            callback.onFailure(e);
        }
    }

    // ==================== VALIDATION ====================

    /**
     * Check if URI is valid image
     */
    public static boolean isValidImageUri(Context context, Uri uri) {
        if (uri == null) return false;

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return false;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            return options.outWidth > 0 && options.outHeight > 0;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get image dimensions without loading full bitmap
     */
    public static int[] getImageDimensions(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            return new int[]{options.outWidth, options.outHeight};
        } catch (IOException e) {
            return null;
        }
    }

    // ==================== CALLBACKS ====================

    /**
     * Callback for upload operations
     */
    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
        void onProgress(int progress);
    }

    /**
     * Callback for delete operations
     */
    public interface DeleteCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Private constructor to prevent instantiation
    private ImageHelper() {
        throw new AssertionError("Cannot instantiate ImageHelper class");
    }
}
