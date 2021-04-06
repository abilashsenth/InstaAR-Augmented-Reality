package com.thenextbiggeek.scribblear;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.googlecode.mp4parser.authoring.Edit;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.CompletionException;

import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity implements Scene.OnPeekTouchListener, Scene.OnUpdateListener {

    private static final int PICK_IMAGE_REQUEST = 27;
    private static final int PICK_VIDEO_REQUEST = 72;
    Context context;
    Session mSession;

    //considers that the user is ready to install the ARCore app by google
    boolean USER_REQUESTED_INSTALL = true;
    ArFragment mArFragment;


    private ModelRenderable mModelRenderable;
    private ViewRenderable textViewRenderable;
    private ViewRenderable textViewRenderable2;
    private ViewRenderable gifViewRenderable;
    private ViewRenderable imageViewRenderable;
    private ViewRenderable videoViewRenderable;
    private ViewRenderable textBoxRenderable;




    String textBoxText;
    private String TAG ="MainActivity";
    private final int STORAGE_PERMISSION_KEY=10;
    private final int CAMERA_PERMISSION_KEY = 9;


    private Material material;
    private final LineSimplifier lineSimplifier = new LineSimplifier();
    private static final Color WHITE = new Color(android.graphics.Color.WHITE);
    private static final float DRAW_DISTANCE = 0.13f;
    private static AnchorNode anchorNode;

    private final ArrayList<Stroke> strokes = new ArrayList<>();
    private Stroke currentStroke;

    ImageView tester;
    GifImageView mGifImageView;
    RelativeLayout mainLayout, gifSelectorLayout;




    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        //hiding the actionBar
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        mainLayout = (RelativeLayout) findViewById(R.id.campreview_relative_layout);
        gifSelectorLayout = (RelativeLayout) findViewById(R.id.gif_chooser_relative_layout);

        //requesting permissions and checking necessary ARCore apk
        requestPermissions();
        int arCoreInstalled = checkIfArCoreInstalled();
        if(arCoreInstalled == 1){
            initUxFragment();
        }else{
            checkIfArCoreInstalled();
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initUxFragment() {

        /**
         * initializes the arFragment, creates a model using modelbuilder. a node represents an AR scene entity
         * hence the model is applied to the node and the node to the fragment
         */

        //textviewrenderable is dummy
        //viewrenderables are declared again in respective functions for some reason only then
        //separate induvidual elements are allowed in s

        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        ViewRenderable.builder().
                setView(this, R.layout.gif_box).
                build().
                thenAccept( viewRenderable -> textViewRenderable = viewRenderable).
                exceptionally(throwable ->
                    {Log.e(TAG, "Unable to load Renderable.", throwable);
                    return null;});

        ViewRenderable.builder().
                setView(this, R.layout.image_box).
                build().
                thenAccept( viewRenderable -> textViewRenderable = viewRenderable).
                exceptionally(throwable ->
                {Log.e(TAG, "Unable to load Renderable.", throwable);
                    return null;});

        ViewRenderable.builder().
                setView(this, R.layout.video_box).
                build().
                thenAccept( viewRenderable -> videoViewRenderable = viewRenderable).
                exceptionally(throwable ->
                {Log.e(TAG, "Unable to load Renderable.", throwable);
                    return null;});

        ViewRenderable.builder().
                setView(this, R.layout.text_box).
                build().
                thenAccept( viewRenderable -> textViewRenderable2 = viewRenderable).
                exceptionally(throwable ->
                {Log.e(TAG, "Unable to load Renderable.", throwable);
                    return null;});








    }

    //once the required gif is selected from the layout
    public void selectedGIFfromLayout(View view) {

        //each of the gifs name and the GifImageView ID are hardcoded and same (not to follow in upcoming versions)
        String resName = view.getResources().getResourceEntryName(view.getId());
        Log.e("RESOURCES", "the name of the drawable selected is " + resName);
        int drawableID = getResId(resName, R.drawable.class);
        gifSelectorLayout.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);
        changeGif(drawableID);
    }

    public static int getResId(String resName, Class<?> c) {

        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void changeGif(int resId) {



        ViewRenderable.builder().
                setView(this, R.layout.gif_box).
                build().
                thenAccept( viewRenderable -> gifViewRenderable = viewRenderable).
                exceptionally(throwable ->
                {Log.e(TAG, "Unable to load Renderable.", throwable);
                    return null;});




                //onTapListener on the arFragment to put in the 3d model
        mArFragment.setOnTapArPlaneListener(
                ((hitResult, plane, motionEvent) -> {
                    if(gifViewRenderable == null){
                        return;
                    }
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode2 = new AnchorNode(anchor);
                    anchorNode2.setParent(mArFragment.getArSceneView().getScene());
                    TransformableNode node2 = new TransformableNode(mArFragment.getTransformationSystem());
                    node2.setParent(anchorNode2);
                    node2.setRenderable(gifViewRenderable);
                    node2.select();

                    mGifImageView = (GifImageView) gifViewRenderable.getView();
                    mGifImageView.setImageResource(resId);
                    //TODO create a dynamic limit such that only 2 or 3 gifs are allowed as nodes
                })
        );

    }

    //accessed when add gif button is clicked in the UI
    public void selectGif(View view) {
        mainLayout.setVisibility(View.GONE);
        gifSelectorLayout.setVisibility(View.VISIBLE);

    }

    //back to the activity_main.xml
    public void cancelGifSelection(View view) {
        gifSelectorLayout.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);

    }

    public void selectTextView(View v){
        String message = popUp();



    }

    private String popUp() {
        final String[] m_Text = {""};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Text to display");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        ViewRenderable.builder().
                setView(this, R.layout.text_box).
                build().
                thenAccept( viewRenderable -> textBoxRenderable = viewRenderable).
                exceptionally(throwable ->
                {Log.e(TAG, "Unable to load Renderable.", throwable);
                    return null;});

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                textBoxText = input.getText().toString();
                Log.e("TEXT", textBoxText);
                TextView tester = (TextView) textBoxRenderable.getView();

                //onTapListener on the arFragment to put in the 3d model
                mArFragment.setOnTapArPlaneListener(
                        ((hitResult, plane, motionEvent) -> {
                            if(textBoxRenderable == null){
                                return;
                            }
                            Anchor anchor = hitResult.createAnchor();
                            AnchorNode anchorNode4 = new AnchorNode(anchor);
                            anchorNode4.setParent(mArFragment.getArSceneView().getScene());


                            TransformableNode node4= new TransformableNode(mArFragment.getTransformationSystem());
                            node4.setParent(anchorNode4);
                            node4.setRenderable(textBoxRenderable);
                            node4.select();


                            tester.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/helvetica.ttf"));
                            tester.setText(textBoxText);
                        })
                );


            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
        return textBoxText;

    }


    @Override
    public void onUpdate(FrameTime frameTime) {
        com.google.ar.core.Camera camera = mArFragment.getArSceneView().getArFrame().getCamera();
        if(camera.getTrackingState() == TrackingState.TRACKING){
            mArFragment.getPlaneDiscoveryController().hide();
        }
    }



    //accessed when image add button is clicked in the UI
    public void startImage(View view) {


        //passes an intent to get image URI
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);

    }

    //accessed when video add button is clicked in the UI
    public void startVideo(View view) {


        //passes an intent to get image URI
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Video"), PICK_VIDEO_REQUEST);

    }


    //when the draw button is clicked
    public void startDrawing(View view) {
        //disable planeRenderer
        mArFragment.getArSceneView().getPlaneRenderer().setEnabled(false);
        mArFragment.getArSceneView().getScene().addOnPeekTouchListener(this);
        mArFragment.getArSceneView().getScene().addOnUpdateListener(this);

        //currently white
        MaterialFactory.makeOpaqueWithColor(this, WHITE).
                thenAccept(material1 -> material = material1.makeCopy()).
                exceptionally(throwable -> {
                    //handle the throwable
                    throw new CompletionException(throwable);
                });


    }

    //draws
    @Override
    public void onPeekTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        Camera camera = mArFragment.getArSceneView().getScene().getCamera();
        Ray ray  = camera.screenPointToRay(motionEvent.getX(), motionEvent.getY());
        Vector3 drawPoint = ray.getPoint(DRAW_DISTANCE);
        if(action == MotionEvent.ACTION_DOWN){
            if(anchorNode == null){
                ArSceneView arSceneView = mArFragment.getArSceneView();
                com.google.ar.core.Camera coreCamera = arSceneView.getArFrame().getCamera();
                if(coreCamera.getTrackingState() != TrackingState.TRACKING){
                    return;
                }
                Pose pose  = coreCamera.getPose();
                anchorNode = new AnchorNode((arSceneView.getSession().createAnchor(pose)));
                anchorNode.setParent(arSceneView.getScene());
            }
            currentStroke = new Stroke(anchorNode, material);
            strokes.add(currentStroke);
            currentStroke.add(drawPoint);
        }else if(action == MotionEvent.ACTION_MOVE && currentStroke != null){
            currentStroke.add(drawPoint);
        }

    }

    public void startRecording(View view) {
        //uses VideoRecorder class
        VideoRecorder mVideoRecorder = new VideoRecorder();
        mVideoRecorder.setSceneView(mArFragment.getArSceneView());

        int orientation  = getResources().getConfiguration().orientation;
        //TODO make quality changeable
        mVideoRecorder.setVideoQuality(CamcorderProfile.QUALITY_720P, orientation);

        boolean recording = mVideoRecorder.onToggleRecord();

        //keeps a timer of 5 seconds and toggles the record stop
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean recording = mVideoRecorder.onToggleRecord();
                String videoPath = mVideoRecorder.getVideoPath().getAbsolutePath();
                Intent intent = new Intent(MainActivity.this, VideoPreviewActivity.class);
                intent.putExtra("VIDEOPATH", videoPath);
                startActivity(intent);
            }
        }, 10000);






    }

    private int checkIfArCoreInstalled() {
        if(mSession == null){
            try {
                switch(ArCoreApk.getInstance().requestInstall(this, USER_REQUESTED_INSTALL)){
                    case INSTALLED:
                        //ARCore installed already. Cool!
                        return 1;

                    case INSTALL_REQUESTED:
                        //Nope //TODO: make sure the user installs the apk for the app's usage
                        USER_REQUESTED_INSTALL = false;
                        break;
                }

            } catch (UnavailableDeviceNotCompatibleException e) {
                Toast.makeText(this, "This smartphone does not support the ARcore APK.", Toast.LENGTH_LONG).show();
                e.printStackTrace();
                return 0;

            } catch (UnavailableUserDeclinedInstallationException e) {
                Toast.makeText(this, "This app requires the ARcore APK to work", Toast.LENGTH_LONG).show();
                e.printStackTrace();
                return 0;

            }

        }

        //returns 0 also when there is an mSession already running
        return 0;

    }

    private void requestPermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            //request for camera permission
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.CAMERA}, CAMERA_PERMISSION_KEY);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            //request for the storage permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_KEY);

        }


    }

    @Override
    public void  onRequestPermissionsResult(int requestCode, String[] permissions,int[] resultCode ){
        switch(requestCode){

            case CAMERA_PERMISSION_KEY:
                if(resultCode.length >0 ){
                    if(resultCode[0] == PackageManager.PERMISSION_GRANTED){
                        //permission granted
                        //TODO make sure the storage permission is also enabled. needed before the final version
                        checkIfArCoreInstalled();
                    }else{
                        //nope
                        Toast.makeText(this, "Camera Permission is required for AR",
                                Toast.LENGTH_SHORT).show();
                        requestPermissions();
                    }
                }
                break;
            case STORAGE_PERMISSION_KEY:
                if(resultCode.length >0){
                    if(resultCode[0] == PackageManager.PERMISSION_GRANTED){
                        //permissiongranted
                        //TODO make sure the camera permission is also enabled. needed before the final version

                    }else{
                        //nope
                        Toast.makeText(this, "Storage Permission is required for VideoReording",
                                Toast.LENGTH_SHORT).show();
                        requestPermissions();
                    }
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();

            try {

                ViewRenderable.builder().
                        setView(this, R.layout.image_box).
                        build().
                        thenAccept( viewRenderable -> imageViewRenderable = viewRenderable).
                        exceptionally(throwable ->
                        {Log.e(TAG, "Unable to load Renderable.", throwable);
                            return null;});


                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                //TODO this is how you refer tothe view. gif or video or image or otherwise


                //onTapListener on the arFragment to put in the 3d model
                mArFragment.setOnTapArPlaneListener(
                        ((hitResult, plane, motionEvent) -> {
                            if(imageViewRenderable == null){
                                return;
                            }
                            Anchor anchor = hitResult.createAnchor();
                            AnchorNode anchorNode3 = new AnchorNode(anchor);
                            anchorNode3.setParent(mArFragment.getArSceneView().getScene());


                            TransformableNode node3 = new TransformableNode(mArFragment.getTransformationSystem());
                            node3.setParent(anchorNode3);
                            node3.setRenderable(imageViewRenderable);
                            node3.select();
                            ImageView tester = (ImageView) imageViewRenderable.getView();
                            tester.setImageBitmap(bitmap);
                        })
                );




            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();

            VideoView tester = (VideoView) videoViewRenderable.getView();
            tester.setVideoURI(uri);

            //onTapListener on the arFragment to put in the 3d model
            mArFragment.setOnTapArPlaneListener(
                    ((hitResult, plane, motionEvent) -> {
                        if(videoViewRenderable == null){
                            return;
                        }
                        Anchor anchor = hitResult.createAnchor();
                        AnchorNode anchorNode4 = new AnchorNode(anchor);
                        anchorNode4.setParent(mArFragment.getArSceneView().getScene());


                        TransformableNode node4 = new TransformableNode(mArFragment.getTransformationSystem());
                        node4.setParent(anchorNode4);
                        node4.setRenderable(videoViewRenderable);
                        node4.select();
                    })
            );


        }
    }

}
