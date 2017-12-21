/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;

import com.almeros.android.multitouch.RotateGestureDetector;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Frame.TrackingState;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PlaneHitResult;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.examples.java.helloar.adapter.ModelSelectorAdapter;
import com.google.ar.core.examples.java.helloar.model.ObjectsModel;
import com.google.ar.core.examples.java.helloar.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer.BlendMode;
import com.google.ar.core.examples.java.helloar.rendering.PlaneAttachment;
import com.google.ar.core.examples.java.helloar.rendering.PlaneRenderer;
import com.google.ar.core.examples.java.helloar.rendering.PointCloudRenderer;
import com.mancj.slideup.SlideUp;
import com.mancj.slideup.SlideUpBuilder;
import com.yarolegovich.discretescrollview.DiscreteScrollView;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using
 * the ARCore API. The application will display any detected planes and will allow the user to
 * tap on a plane to place a 3d model of the Android robot.
 */
public class HelloArActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = HelloArActivity.class.getSimpleName();
    private static final int LIMIT_OF_OBJECTS = 32;

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView mSurfaceView;

    private Config mDefaultConfig = null;
    private Session mSession;
    private BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private Snackbar mLoadingMessageSnackbar = null;

    private PlaneRenderer mPlaneRenderer = new PlaneRenderer();
    private PointCloudRenderer mPointCloud = new PointCloudRenderer();


    private static final String[][] mModelsInfo = new String[][]{
            new String[]{"Snorlax", "snorlax.obj", "PM_Kabigon12901.png", "0.08","snorlax_preview.png"},
            new String[]{"Tauros", "tauros.obj", "PM_Kentaros4282.png", "0.03","tauros_preview.png"},
            new String[]{"Pacman Trophy", "pactrophy.obj", "Tex_0209_0.png", "0.04","pactrophy_preview.png"},
            new String[]{"Mario Trophy", "mariocu.obj", "Tex_0213_0.png", "0.04","mariocu_preview.png"},
            new String[]{"Spider", "celspder.obj", "celspder.png", "5.0","celspder_preview.png"},
            new String[]{"Android", "andy.obj", "andy.png", "1.0","andy_preview.png"},
    };
    private ObjectRenderer[] mVirtualObjectArr = new ObjectRenderer[mModelsInfo.length];
    private ObjectsModel[] mModels = new ObjectsModel[mModelsInfo.length];
    private ObjectsModel mCurrentSelectedModel;
    private float mCurrentScaleFactor = 1.0F;
    private static final float mModelScaleFactorChange = 0.03f;

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] mAnchorMatrix = new float[16];

    // Tap handling and UI.
    private ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(16);
    private ArrayList<PlaneAttachment> mTouches = new ArrayList<>();

    //Permissions
    private static boolean hasAllPermissionsBeenGranted = false;
    private static final int PERMISSION_DENIED = -1;
    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private static final String[] PERMISSIONS_REQUIRED = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private boolean needSpaceShareImage = false;
    private Bitmap spaceShareImage;

    //Menu
    private ImageView infoIcon;
    private ImageView resetIcon;
    private ImageView undoIcon;
    private ImageView shareIcon;

    //Views
    private ImageView slideUpArrow;
    private ImageView cancelCross;
    private RelativeLayout slideView;
    private SlideUp slideUp;
    private DiscreteScrollView scrollView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, PERMISSIONS_REQUIRED,
                REQUEST_PERMISSIONS_CODE);

        for (int i = 0; i < mModelsInfo.length; i++) {
            String[] modelInfo = mModelsInfo[i];

            Bitmap previewImg = getBitmapFromAsset(this, modelInfo[4] );
            ObjectsModel obj = new ObjectsModel(modelInfo[0],
                    modelInfo[1],
                    modelInfo[2],
                    Float.parseFloat(modelInfo[3]));
            obj.setPreview(previewImg);
            mCurrentSelectedModel = obj;
            mModels[i] = obj;
        }


        infoIcon = (ImageView) findViewById(R.id.infoIcon);
        resetIcon = (ImageView) findViewById(R.id.resetIcon);
        undoIcon = (ImageView) findViewById(R.id.undoIcon);
        shareIcon = (ImageView) findViewById(R.id.shareIcon);
        infoIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInfo();
            }
        });
        resetIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetObjects();
            }
        });
        undoIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                undoObject();
            }
        });
        shareIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                needSpaceShareImage = true;
            }
        });


        scrollView = findViewById(R.id.picker);
        cancelCross = (ImageView) findViewById(R.id.cancelModelPicker);
        slideUpArrow = (ImageView) findViewById(R.id.slideUpArrow);
        slideView = (RelativeLayout) findViewById(R.id.slideView);
        slideUp = new SlideUpBuilder(slideView)
                .withStartState(SlideUp.State.HIDDEN)
                .withStartGravity(Gravity.BOTTOM)
                .withLoggingEnabled(true)
                .build();
        scrollView.setAdapter(new ModelSelectorAdapter(mModels, this));

        slideUpArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //The following lines of code fix an intermittent problem with
                //slideUp menu where it was not appearing intermittently
                slideUp.show();
                slideView.requestLayout();
                slideView.requestFocus();
                scrollView.requestLayout();
                scrollView.requestFocus();
                slideUp.show();

                scrollView.smoothScrollToPosition(1);
                slideUpArrow.setVisibility(View.INVISIBLE);
            }
        });
        cancelCross.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slideUp.hideImmediately();
                slideUpArrow.setVisibility(View.VISIBLE);
            }
        });

        mSurfaceView = (GLSurfaceView) findViewById(R.id.surfaceview);

        mSession = new Session(/*context=*/this);

        // Create default config, check is supported, create session from that config.
        mDefaultConfig = Config.createDefaultConfig();
        if (!mSession.isSupported(mDefaultConfig)) {
            Toast.makeText(this, "This device does not support AR", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set up tap listener.
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onSingleTap(e);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });




        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleGestureDetector.onTouchEvent(event);
                return mGestureDetector.onTouchEvent(event);
            }
        });


        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {

                float scaleFactor = detector.getScaleFactor();

                if (scaleFactor > 1) {
                    mCurrentScaleFactor += mCurrentScaleFactor * mModelScaleFactorChange * scaleFactor * scaleFactor;
                } else {
                    mCurrentScaleFactor -= mCurrentScaleFactor * mModelScaleFactorChange * scaleFactor * scaleFactor;
                }
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
            }
        });

        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }


    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        if(hasAllPermissionsBeenGranted){
            mSession.resume(mDefaultConfig);
            mSurfaceView.onResume();
            resetObjects();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call mSession.update() and get a SessionPausedException.
        mSurfaceView.onPause();
        mSession.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        boolean tempPermissionBeenGranted = true;
        for(int result : results){
            if(result == PERMISSION_DENIED) tempPermissionBeenGranted = false;
        }
        hasAllPermissionsBeenGranted = tempPermissionBeenGranted;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            //Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onSingleTap(MotionEvent e) {
        // Queue tap if there is space. Tap is lost if queue is full.
        mQueuedSingleTaps.offer(e);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        mBackgroundRenderer.createOnGlThread(/*context=*/this);
        mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());

        // Prepare the other rendering objects.
        try {
            for (int i = 0; i < mVirtualObjectArr.length; i++) {
                mVirtualObjectArr[i] = new ObjectRenderer(mModels[i]);
                mVirtualObjectArr[i].createOnGlThread(/*context=*/this, mModels[i].getObjectFileName(), mModels[i].getDiffuseTextureFileName());
                mVirtualObjectArr[i].setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }
        try {
            mPlaneRenderer.createOnGlThread(/*context=*/this, "trigrid.png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }
        mPointCloud.createOnGlThread(/*context=*/this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mSession.setDisplayGeometry(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = mSession.update();

            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.
            MotionEvent tap = mQueuedSingleTaps.poll();
            if (tap != null && frame.getTrackingState() == TrackingState.TRACKING) {
                for (HitResult hit : frame.hitTest(tap)) {
                    // Check if any plane was hit, and if it was hit inside the plane polygon.
                    if (hit instanceof PlaneHitResult && ((PlaneHitResult) hit).isHitInPolygon()) {
                        // Cap the number of objects created. This avoids overloading both the
                        // rendering system and ARCore.
                        if (mTouches.size() >= LIMIT_OF_OBJECTS) {
                            mSession.removeAnchors(Arrays.asList(mTouches.get(0).getAnchor()));
                            mTouches.remove(0);
                        }
                        // Adding an Anchor tells ARCore that it should track this position in
                        // space. This anchor will be used in PlaneAttachment to place the 3d model
                        // in the correct position relative both to the world and to the plane.
                        PlaneAttachment planeAttachment = new PlaneAttachment(
                                ((PlaneHitResult) hit).getPlane(),
                                mSession.addAnchor(hit.getHitPose()),
                                mCurrentSelectedModel,
                                mCurrentSelectedModel.getScaleFactor()
                        );
                        mTouches.add(planeAttachment);

                        // Hits are sorted by depth. Consider only closest hit on a plane.
                        break;
                    }
                }
            }

            // Draw background.
            mBackgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (frame.getTrackingState() == TrackingState.NOT_TRACKING) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            mSession.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            frame.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            // Visualize tracked points.
            mPointCloud.update(frame.getPointCloud());
            mPointCloud.draw(frame.getPointCloudPose(), viewmtx, projmtx);

            // Check if we detected at least one plane. If so, hide the loading message.
            if (mLoadingMessageSnackbar != null) {
                for (Plane plane : mSession.getAllPlanes()) {
                    if (plane.getType() == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING &&
                            plane.getTrackingState() == Plane.TrackingState.TRACKING) {
                        hideLoadingMessage();
                        break;
                    }
                }
            }

            // Visualize planes.
            mPlaneRenderer.drawPlanes(mSession.getAllPlanes(), frame.getPose(), projmtx);

            // Visualize anchors created by touch.
            for (int i = 0; i < mTouches.size(); i++) {
                PlaneAttachment touchAttachment = mTouches.get(i);
                if (!touchAttachment.isTracking()) {
                    continue;
                }

                // Get the current combined pose of an Anchor and Plane in world space. The Anchor
                // and Plane poses are updated during calls to session.update() as ARCore refines
                // its estimate of the world.
                touchAttachment.getPose().toMatrix(mAnchorMatrix, 0);

                if(i == mTouches.size() - 1 && touchAttachment.getModel().getName().equals(mCurrentSelectedModel.getName()))
                    touchAttachment.setScaleFactor(mCurrentScaleFactor);

                for(ObjectRenderer virutalObject : mVirtualObjectArr){
                    if(virutalObject.getModel().getName().equals(touchAttachment.getModel().getName())){
                        virutalObject.updateModelMatrix(mAnchorMatrix, touchAttachment.getScaleFactor());
                        virutalObject.draw(viewmtx, projmtx, lightIntensity);
                        break;
                    }
                }

                // Update and draw the model and its shadow.
//                mVirtualObject.updateModelMatrix(mAnchorMatrix, mModelScaleFactor);
//                mVirtualObjectShadow.updateModelMatrix(mAnchorMatrix, mCurrentScaleFactor);
//                mVirtualObject.draw(viewmtx, projmtx, lightIntensity);
//                mVirtualObjectShadow.draw(viewmtx, projmtx, lightIntensity);
            }



            if(needSpaceShareImage){
                needSpaceShareImage = false;
                spaceShareImage = saveTexture(mSurfaceView.getWidth(), mSurfaceView.getHeight());
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, getImageUri(this, spaceShareImage));
                shareIntent.setType("image/jpeg");
                startActivity(Intent.createChooser(shareIntent, "Share With"));
            }


        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    private void showLoadingMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingMessageSnackbar = Snackbar.make(
                        HelloArActivity.this.findViewById(android.R.id.content),
                        "Searching for surfaces...", Snackbar.LENGTH_INDEFINITE);
                mLoadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
                mLoadingMessageSnackbar.show();
            }
        });
    }

    private void hideLoadingMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingMessageSnackbar.dismiss();
                mLoadingMessageSnackbar = null;
            }
        });
    }


    public void onClickModel(ObjectsModel obj, int position) {
        mCurrentSelectedModel = obj;
        mCurrentScaleFactor = obj.getScaleFactor();
    }

    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();
        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
        }

        return bitmap;
    }



    private void undoObject(){
        if(mTouches.size() <= 0) return;
        mTouches.remove(mTouches.size() - 1);
    }

    private void resetObjects(){
        if(mTouches.size() <= 0) return;
        mTouches.clear();
    }

    private void showInfo(){
        new AlertDialog.Builder(this)
                .setTitle("AR Objects (An ARCore Experiment)")
                .setMessage("AR Objects is an experiment based on the ARCore features provided in the ARCore Preivew Release. \n" +
                            "This app allows you to place objects on a surface or in air, delete and undo, and share your space with friends \n" +
                            "Augument your space with this app. \n\n\n" +
                            "An App By Zunair Syed")
                .setPositiveButton("Good Stuff !", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }



    public static Bitmap saveTexture(int width, int height) {
        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        reverseBuffer(buffer, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    private static void reverseBuffer(ByteBuffer buf, int width, int height)
    {
        int i = 0;
        byte[] tmp = new byte[width * 4];
        while (i++ < height / 2)
        {
            buf.get(tmp);
            System.arraycopy(buf.array(), buf.limit() - buf.position(), buf.array(), buf.position() - width * 4, width * 4);
            System.arraycopy(tmp, 0, buf.array(), buf.limit() - buf.position(), width * 4);
        }
        buf.rewind();
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        //TODO: ADD WRITE PERMISSON
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

}
