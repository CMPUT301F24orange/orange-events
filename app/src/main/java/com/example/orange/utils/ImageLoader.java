//package com.example.orange.utils;
//
//import android.content.Context;
//import android.widget.ImageView;
//
//import com.bumptech.glide.Glide;
//import com.google.firebase.firestore.Blob;
//
///**
// * ImageLoader handles loading images into ImageViews using Glide.
// *
// * @deprecated
// *  Currently still referenced in code just in case we come back to using but if statements block it from being used functionally
// */
//public class ImageLoader {
//    /**
//     * Loads an image into an ImageView using Glide.
//     *
//     * @param context   The context of the calling component.
//     * @param imageUrl  The URL of the image to load.
//     * @param imageView The ImageView where the image will be displayed.
//     */
//    public static void loadImage(Context context, String imageUrl, ImageView imageView) {
//        Glide.with(context)
//                .load(imageUrl)
////                .placeholder(R.drawable.default_profile)
//                .into(imageView);
//    }
//
//    /**
//     * Loads an image from byte[] data into an ImageView using Glide.
//     *
//     * @param context   The context of the calling component.
//     * @param imageData The byte[] data of the image to load.
//     * @param imageView The ImageView where the image will be displayed.
//     */
//    public static void loadImage(Context context, byte[] imageData, ImageView imageView) {
//        Glide.with(context)
//                .load(imageData)
////                .placeholder(R.drawable.default_profile)
//                .into(imageView);
//    }
//
//    /**
//     * Loads an image from Blob data into an ImageView using Glide.
//     *
//     * @param context   The context of the calling component.
//     * @param imageBlob The Blob data of the image to load.
//     * @param imageView The ImageView where the image will be displayed.
//     */
//    public static void loadImage(Context context, Blob imageBlob, ImageView imageView) {
//        if (imageBlob != null) {
//            Glide.with(context)
//                    .load(imageBlob.toBytes())
////                    .placeholder(R.drawable.default_profile)
//                    .into(imageView);
//        } else {
////            imageView.setImageResource(R.drawable.default_profile);
//        }
//    }
//}
