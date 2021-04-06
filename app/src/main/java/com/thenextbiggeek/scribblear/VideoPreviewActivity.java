package com.thenextbiggeek.scribblear;

import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class VideoPreviewActivity extends AppCompatActivity {

    private static final int AUDIO_REQUEST_CODE = 5;
    private static final String TAG = "MUX" ;
    VideoView mVideoView;
    private long videoDuration;
    private Uri videoUri;
    private Uri audioUri;
    private String[] cmd;
    String output;
    boolean audioCombined;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview);
        mVideoView = (VideoView) findViewById(R.id.videoView);
        Intent intent = getIntent();
        String location = intent.getStringExtra("VIDEOPATH");
        Toast.makeText(this, location, Toast.LENGTH_LONG).show();
        videoUri = Uri.fromFile(new File(location));
        mVideoView.setVideoURI(videoUri);
        mVideoView.start();
        audioCombined = false;
    }

    public void getAudio(View view) {
        //to get the duration of the video in long format
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(this,videoUri );
        String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        videoDuration  = Long.parseLong(time);
        mediaMetadataRetriever.release();

        //intent to get audio
        Intent intent_upload = new Intent();
        intent_upload.setType("audio/*");
        intent_upload.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent_upload,AUDIO_REQUEST_CODE);

    }

    ProgressBar mProgressbar;



    private void playImposedFile(String s) {
        //bring in the file, play em in the VideoView
        mVideoView.setVideoPath(s);
        mVideoView.start();
        Toast.makeText(this, "videoStarted", Toast.LENGTH_SHORT).show();


    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == AUDIO_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                audioUri = data.getData();
                audioCombined = true;
                Log.e("AUDIO URI", audioUri.toString());

                String audio = getPath(audioUri);
                String root = Environment.getExternalStorageDirectory().toString();
                String video = (videoUri.getPath());

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                File outputVideoDirectory = new File(root+"/"+"InstaAR");
                if(!outputVideoDirectory.exists()){
                    outputVideoDirectory.mkdirs();
                }
                output = root + "/" + "InstaAR" +  "/" + "InstaAR"+ timeStamp+".mp4";
                mProgressbar = findViewById(R.id.progress_circular);
                combineAudioVideo(video, audio, output);

            }
        }


        super.onActivityResult( requestCode,  resultCode,  data);
    }

    private void combineAudioVideo(String mp4location, String mp3location, String outputlocation) {

        //ffmpeg -i video.mp4 -i audio.mp3 -shortest output.mp4


       AsyncTaskFFMPEG mCombiner = new AsyncTaskFFMPEG();
       String[] mArray = new String[]{mp4location, mp3location, outputlocation};
       mCombiner.execute(mArray);
    }



    private String getPath(Uri uri) {
        String[]  data = { MediaStore.Audio.Media.DATA };
        CursorLoader loader = new CursorLoader(this, uri, data, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public void cancelShare(View view) {
        //cancels the entire activity and goes to MainActivity
        Intent intent = new Intent(VideoPreviewActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void shareVideo(View view) {

        if(audioCombined){
            //TODO share intent
        }

    }


    public class  AsyncTaskFFMPEG extends AsyncTask<String[], Void, String>{

        @Override
        protected String doInBackground(String[]... strings) {


            String[] mArray = strings[0];

            String mp4location = mArray[0];
            String mp3location = mArray[1];
            String outputlocation = mArray[2];

            Log.e("async", "mp4location"+ mp4location + "mp3loc" + mp3location +
                    "output "+  outputlocation);

            cmd = new String[]{"-i", mp4location , "-i",  mp3location, "-shortest", "-preset",
                    "ultrafast", outputlocation};


            //initialize ffmpeg
            FFmpeg fFmpeg = FFmpeg.getInstance(VideoPreviewActivity.this);
            try {
                fFmpeg.loadBinary(new LoadBinaryResponseHandler(){
                    @Override
                    public void onStart() {

                        Log.e("FFMPEG", "ffmpeg started");

                    }

                    @Override
                    public void onFailure() {
                        Log.e("FFMPEG", "ffmpeg started and failed");

                    }

                    @Override
                    public void onSuccess() {

                        Log.e("FFMPEG", "ffmpeg started successfully");

                    }

                    @Override
                    public void onFinish() {

                        Log.e("FFMPEG", "ffmpeg finished started");

                    }

                });
            }catch (Exception e){
                //handle exception
            }
            //execute binary
            try{
                fFmpeg.execute(cmd, new ExecuteBinaryResponseHandler(){
                    @Override
                    public void onStart() {
                        Log.e("FFMPEG","command executing");
                    }

                    @Override
                    public void onProgress(String message) {
                        //Toast.makeText(VideoPreviewActivity.this, "command on progress", Toast.LENGTH_SHORT).show();

                        int i =0;
                        Log.e("FFMPEG", "process running"+ ++i);
                    }

                    @Override
                    public void onFinish() {
                        Log.e("ASYNCTASK", "process done in async");
                        Log.e("ASYNCTASK", "the output location is  " + outputlocation);
                        mProgressbar.setVisibility(View.INVISIBLE);
                        playImposedFile(outputlocation);


                    }

                });
            }catch (Exception e){

            }
            return outputlocation;

        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            Log.e("ASYNCTASK", "process running in async");
            mProgressbar.setVisibility(View.VISIBLE);


        }
    }





}
