package com.esafirm.sample;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.esafirm.imagepicker.features.ImagePickerConfig;
import com.esafirm.imagepicker.features.ImagePickerFragment;
import com.esafirm.imagepicker.features.ImagePickerInteractionListener;
import com.esafirm.imagepicker.features.cameraonly.CameraOnlyConfig;
import com.esafirm.imagepicker.helper.ConfigUtils;
import com.esafirm.imagepicker.helper.IpLogger;
import com.esafirm.imagepicker.helper.LocaleManager;
import com.esafirm.imagepicker.helper.ViewUtils;
import com.esafirm.imagepicker.model.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * This custom UI for ImagePicker puts the picker in the bottom half of the screen, and a preview of
 * the last selected image in the top half.
 */
public class CustomUIActivity extends AppCompatActivity implements ImagePickerInteractionListener {

    private ActionBar actionBar;
    private ImageView photoPreview;
    private LinearLayout pageLayout;
    private FrameLayout imagePickerFragmentContainer;
    private ImagePickerFragment imagePickerFragment;

    private CameraOnlyConfig cameraOnlyConfig;
    private ImagePickerConfig config;
    private int selectedImageCount = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.updateResources(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        config = getIntent().getExtras().getParcelable(ImagePickerConfig.class.getSimpleName());
        cameraOnlyConfig = getIntent().getExtras().getParcelable(CameraOnlyConfig.class.getSimpleName());
        setTheme(config.getTheme());
        setContentView(R.layout.activity_custom_ui);

        if (savedInstanceState != null) {
            // The fragment has been restored.
            IpLogger.getInstance().e("Fragment has been restored");
            imagePickerFragment = (ImagePickerFragment) getSupportFragmentManager().findFragmentById(R.id.ef_imagepicker_fragment_placeholder);
        } else {
            IpLogger.getInstance().e("Making fragment");
            imagePickerFragment = ImagePickerFragment.newInstance(config, cameraOnlyConfig);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.ef_imagepicker_fragment_placeholder, imagePickerFragment);
            ft.commit();
        }
        setupView();
    }

    /**
     * Create options menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.esafirm.imagepicker.R.menu.ef_image_picker_menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuCamera = menu.findItem(com.esafirm.imagepicker.R.id.menu_camera);
        if (menuCamera != null) {
            if (config != null) {
                menuCamera.setVisible(config.isShowCamera());
            }
        }

        MenuItem menuDone = menu.findItem(com.esafirm.imagepicker.R.id.menu_done);
        if (menuDone != null) {
            menuDone.setTitle(ConfigUtils.getDoneButtonText(this, config));
            menuDone.setVisible(imagePickerFragment.isShowDoneButton());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Handle options menu's click event.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (id == com.esafirm.imagepicker.R.id.menu_done) {
            imagePickerFragment.onDone();
            return true;
        }
        if (id == com.esafirm.imagepicker.R.id.menu_camera) {
            imagePickerFragment.captureImageWithPermission();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * If you're content for the back button to take you back, without consulting the state of the
     * fragment, you don't need to override this. On the other hand, the ImagePickerFragment might
     * want to handle the back button. For example, if it's in folder mode and a folder has been
     * selected, the fragment will go back to the folder list if you call its handleBack().
     */
    @Override
    public void onBackPressed() {
        if (!imagePickerFragment.handleBack()) {
            super.onBackPressed();
        }
    }

    private void setupView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        if (actionBar != null) {
            final Drawable arrowDrawable = ViewUtils.getArrowIcon(this);
            final int arrowColor = config.getArrowColor();
            if (arrowColor != ImagePickerConfig.NO_COLOR && arrowDrawable != null) {
                arrowDrawable.setColorFilter(arrowColor, PorterDuff.Mode.SRC_ATOP);
            }
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(arrowDrawable);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        photoPreview = findViewById(R.id.photo_preview);
        imagePickerFragmentContainer = findViewById(R.id.ef_imagepicker_picker_container);
        pageLayout = findViewById(R.id.page_layout);
        selectionChanged(new ArrayList<>());
    }

    /* --------------------------------------------------- */
    /* > ImagePickerInteractionListener Methods */
    /* --------------------------------------------------- */

    @Override
    public void setTitle(String title) {
        actionBar.setTitle(title);
    }

    @Override
    public void cancel() {
        finish();
    }

    @Override
    public void selectionChanged(List<Image> imageList) {
        if (imageList.isEmpty()) {
            // When no images are currently selected, we remove the image preview (if present). In
            // landscape mode, it was to the left of the picker, which means that we need to
            // increase the number of columns when we're in landscape mode to 3.
            // We skip this if there were never any selected images.
            if (selectedImageCount > 0) {
                imagePickerFragment.setColumnNumbers(3, 5, 2, 4);
            }
            if (photoPreview.getParent() != null) {
                pageLayout.removeView(photoPreview);
            }
        } else {
            // When an image has been selected, and previously there was no image selected, we
            // add the image preview. In landscape mode, we add it to the left of the picker, which
            // means that we need to reduce the number of columns when we're in landscape mode to 3.
            if (selectedImageCount == 0) {
                imagePickerFragment.setColumnNumbers(3, 3, 2, 2);
            }
            final Handler handler = new Handler();
            if (photoPreview.getParent() == null) {
                pageLayout.addView(photoPreview, 0);
            }
            handler.postDelayed(() -> {
                photoPreview.setImageBitmap(BitmapFactory.decodeFile(imageList.get(imageList.size() - 1).getPath()));
            }, 0);
        }
        selectedImageCount = imageList.size();
    }

    @Override
    public void finishPickImages(Intent result) {
        setResult(RESULT_OK, result);
        finish();
    }
}