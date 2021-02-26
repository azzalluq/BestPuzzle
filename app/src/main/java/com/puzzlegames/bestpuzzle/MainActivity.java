package com.puzzlegames.bestpuzzle;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private RecyclerView mRecyclerView;
    private ArrayList<Drawable> savedPhotos = new ArrayList<>();
    private ArrayList<String> photoPaths = new ArrayList<>();
    private int mGridRows;
    private boolean defaultAdapter;
    private static int REQUEST_PHOTO_CROPPING = 1;


    private Drawable scalePhoto(int viewSize, String photopath) {
        // scale image previews to fit the allocated View to save app memory
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photopath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = Math.min(photoW/viewSize, photoH/viewSize);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        Bitmap bitmap = BitmapFactory.decodeFile(photopath, bmOptions);
        return new BitmapDrawable(getResources(), bitmap);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final int[] drawableInts = {
                R.drawable.image_1, R.drawable.image_2, R.drawable.image_3,
                R.drawable.image_4, R.drawable.image_5, R.drawable.image_6,
                R.drawable.image_7, R.drawable.image_8, R.drawable.image_9,
                R.drawable.image_10, R.drawable.images_11, R.drawable.images_12,
                R.drawable.images_13
        };

        getSavedPhotos();
        mRecyclerView = findViewById(R.id.pictureRecyclerView);

        mRecyclerView.setHasFixedSize(true);
        int orientation = getResources().getConfiguration().orientation;

        RecyclerView.LayoutManager layoutManager;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
             layoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        } else {
            layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        }
        mRecyclerView.setLayoutManager(layoutManager);

        final ImageRecyclerAdapter testAdapter = new ImageRecyclerAdapter(drawableInts,this);
        mRecyclerView.setAdapter(testAdapter);
        defaultAdapter = true;

        final RadioGroup setGrid = findViewById(R.id.setGrid);
        setGrid.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                for (int x = 0; x<4; x++) {
                    RadioButton radioButton = (RadioButton)group.getChildAt(x);
                    if (radioButton.getId() == checkedId) {
                        mGridRows = x + 3;  // update grid size for use in load button listener in this context
                        testAdapter.setmGridRows(x + 3);  // send the value to recycler adapter for use in button listener there
                        break;
                    }
                }
            }
        });

        final Intent gameIntent = new Intent(this, PuzzleActivity.class);
        mGridRows = 4;

        GifImageView loadButton = findViewById(R.id.playButtonId);
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameIntent.putExtra("numColumns", mGridRows);  // set extra for grid size

                startActivity(gameIntent);  // start game activity
                finish();
            }
        });

    }

    /**
     * Search the app picture directory for photos of .jpg or .png type and store them in instance variables as
     * drawables (image) and strings (filepath) which can be used to display the photos and send to the Puzzle Activity
     */
    private void getSavedPhotos() {
        File imageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] storedImages = imageDir.listFiles();  //TODO: why warning - even when no files no exception
        if (storedImages == null) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        long recyclerViewPx = Math.round(150 * density);
        // checks for files in the apps image directory which are not of the jpg or png type
        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                boolean acceptedType = false;
                String pathName = pathname.getName();
                if (pathName.endsWith(".jpg") || pathName.endsWith(".png"))
                    acceptedType = true;
                return acceptedType;
            }
        };

        for (File file : storedImages) {
            // check for empty or wrong file types and delete them
            if (file.length() == 0 || !fileFilter.accept(file)) {
                boolean deletedFile = file.delete();
            } else {
                String imagePath = file.getAbsolutePath();
                photoPaths.add(imagePath);
                Drawable imageBitmap = scalePhoto((int)recyclerViewPx, imagePath);
                savedPhotos.add(imageBitmap);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //TODO: start photocropping with code then when press back set photo data as list of paths then update here
        //  also consider case where cropper goes to game, then back to here directly from solved UI
        Log.i(TAG, "requestCode: "+requestCode);
        Log.i(TAG, "resultCode: "+resultCode);
        if (requestCode == REQUEST_PHOTO_CROPPING && resultCode == RESULT_OK && data != null) {
            // must update recycler view adapter for photos, as new photos may have been loaded, notify adapter data changed
            ArrayList<String> newPhotos= data.getStringArrayListExtra("savedPhotos");
            float density = getResources().getDisplayMetrics().density;
            long recyclerViewPx = Math.round(150 * density);
            // create bitmap for each new photo and add to adapters data set
            for (String photoPath : newPhotos) {
                if (photoPath != null) {  // just to be sure no null paths sneak in
                    photoPaths.add(photoPath);
                    Drawable imageBitmap = scalePhoto((int)recyclerViewPx, photoPath);
                    savedPhotos.add(imageBitmap);
                }
            }
            if (mRecyclerView.getAdapter() != null) {
                mRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }
    }
}
