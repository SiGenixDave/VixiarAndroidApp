package com.vixiar.indicor.Activities;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import com.vixiar.indicor.R;

public class VideoActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        VideoView v = findViewById(R.id.videoView);

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(v);
        v.setMediaController(mediaController);
        String path = "android.resource://" + getPackageName() + "/" + R.raw.vixvideo;
        v.setVideoURI(Uri.parse(path));
        v.start();
    }
}
