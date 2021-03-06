package com.google.vrtoolkit.cardboard.samples.simplevideowidget;

import com.google.vrtoolkit.cardboard.widgets.video.VrVideoEventListener;
import com.google.vrtoolkit.cardboard.widgets.video.VrVideoView;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class SimpleVrVideoActivity extends Activity {
  private static final String TAG = SimpleVrVideoActivity.class.getSimpleName();

  /**
   * Preserve the video's state when rotating the phone.
   */
  private static final String STATE_IS_PAUSED = "isPaused";
  private static final String STATE_PROGRESS_TIME = "progressTime";
  /**
   * The video duration doesn't need to be preserved, but it is saved in this example. This allows
   * the seekBar to be configured during {@link #onRestoreInstanceState(Bundle)} rather than waiting
   * for the video to be reloaded and analyzed. This avoid UI jank.
   */
  private static final String STATE_VIDEO_DURATION = "videoDuration";

  /**
   * Arbitrary variable to track load status. In this example, this variable should only be accessed
   * on the UI thread. In a real app, this variable would be code that performs some UI actions when
   * the video is fully loaded.
   */
  private boolean loadVideoSuccessful = false;
  public boolean isLoadVideoSuccessful() {
    return loadVideoSuccessful;
  }

  /** Tracks the file to be loaded across the lifetime of this app. **/
  private Uri fileUri;
  private VideoLoaderTask backgroundVideoLoaderTask;

  /**
   * The video view and its custom UI elements.
   */
  private VrVideoView videoWidgetView;

  /**
   * Seeking UI & progress indicator. The seekBar's progress value represents milliseconds in the
   * video.
   */
  private SeekBar seekBar;
  private TextView statusText;


  private boolean isPaused = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_simplevrvideo);

    seekBar = (SeekBar) findViewById(R.id.seek_bar);
    seekBar.setOnSeekBarChangeListener(new SeekBarListener());
    statusText = (TextView) findViewById(R.id.status_text);


    // Bind input and output objects for the view.
    videoWidgetView = (VrVideoView) findViewById(R.id.video_view);
    videoWidgetView.setEventListener(new ActivityEventListener());

    // Initial launch of the app or an Activity recreation due to rotation.
    handleIntent(getIntent());
  }

  /**
   * Called when the Activity is already running and it's given a new intent.
   */
  @Override
  protected void onNewIntent(Intent intent) {
    Log.i(TAG, this.hashCode() + ".onNewIntent()");
    // Save the intent. This allows the getIntent() call in onCreate() to use this new Intent during
    // future invocations.
    setIntent(intent);
    // Load the new image.
    handleIntent(intent);
  }

  /**
   * Load custom videos based on the Intent or load the default video. See the Javadoc for this
   * class for information on generating a custom intent via adb.
   */
  private void handleIntent(Intent intent) {
    // Determine if the Intent contains a file to load.
    if (Intent.ACTION_VIEW.equals(intent.getAction())) {
      Log.i(TAG, "ACTION_VIEW Intent recieved");

      fileUri = intent.getData();
      if (fileUri == null) {
        Log.w(TAG, "No data uri specified. Use \"-d /path/filename\".");
      } else {
        Log.i(TAG, "Using file " + fileUri.toString());
      }
    } else {
      Log.i(TAG, "Intent is not ACTION_VIEW. Using the default video.");
      fileUri = null;
    }

    // Load the bitmap in a background thread to avoid blocking the UI thread. This operation can
    // take 100s of milliseconds.
    if (backgroundVideoLoaderTask != null) {
      // Cancel any task from a previous intent sent to this activity.
      backgroundVideoLoaderTask.cancel(true);
    }
    backgroundVideoLoaderTask = new VideoLoaderTask();
    backgroundVideoLoaderTask.execute(fileUri);
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putLong(STATE_PROGRESS_TIME, videoWidgetView.getCurrentPosition());
    savedInstanceState.putLong(STATE_VIDEO_DURATION, videoWidgetView.getDuration());
    savedInstanceState.putBoolean(STATE_IS_PAUSED, isPaused);
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  public void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    long progressTime = savedInstanceState.getLong(STATE_PROGRESS_TIME);
    videoWidgetView.seekTo(progressTime);
    seekBar.setMax((int) savedInstanceState.getLong(STATE_VIDEO_DURATION));
    seekBar.setProgress((int) progressTime);

    isPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED);
    if (isPaused) {
      videoWidgetView.pauseVideo();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    // Prevent the view from rendering continuously when in the background.
    videoWidgetView.pauseRendering();
    // If the video was playing when onPause() iss called, the default behavior will be to pause
    // the video and keep it paused when onResume() is called.
    isPaused = true;
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Resume the 3D rendering.
    videoWidgetView.resumeRendering();
    // Update the text to account for the paused video in onPause().
    updateStatusText();
  }

  @Override
  protected void onDestroy() {
    // Destroy the widget and free memory.
    videoWidgetView.shutdown();
    super.onDestroy();
  }

  private void togglePause() {
    if (isPaused) {
      videoWidgetView.playVideo();
    } else {
      videoWidgetView.pauseVideo();
    }
    isPaused = !isPaused;
    updateStatusText();
  }

  private void updateStatusText() {
    StringBuilder status = new StringBuilder();
    status.append(isPaused ? "Paused: " : "Playing: ");
    status.append(String.format("%.2f", videoWidgetView.getCurrentPosition() / 1000f));
    status.append(" / ");
    status.append(videoWidgetView.getDuration() / 1000f);
    status.append(" seconds.");
    statusText.setText(status.toString());
  }

  /**
   * When the user manipulates the seek bar, update the video position.
   */
  private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      if (fromUser) {
        videoWidgetView.seekTo(progress);
        updateStatusText();
      } // else this was from the ActivityEventHandler.onNewFrame()'s seekBar.setProgress update.
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }
  }

  /**
   * Listen to the important events from widget.
   */
  private class ActivityEventListener extends VrVideoEventListener  {
    /**
     * Called by video widget on the UI thread when it's done loading the video.
     */
    @Override
    public void onLoadSuccess() {
      Log.i(TAG, "Sucessfully loaded video " + videoWidgetView.getDuration());
      loadVideoSuccessful = true;
      seekBar.setMax((int) videoWidgetView.getDuration());

      updateStatusText();
    }

    /**
     * Called by video widget on the UI thread on any asynchronous error.
     */
    @Override
    public void onLoadError(String errorMessage) {
      // An error here is normally due to being unable to decode the video format.
      loadVideoSuccessful = false;
      Toast.makeText(
          SimpleVrVideoActivity.this, "Error loading video: " + errorMessage, Toast.LENGTH_LONG)
          .show();
      Log.e(TAG, "Error loading video: " + errorMessage);
    }

    @Override
    public void onClick() {
      togglePause();
    }

    /**
     * Update the UI every frame.
     */
    @Override
    public void onNewFrame() {
      updateStatusText();
      seekBar.setProgress((int) videoWidgetView.getCurrentPosition());
    }

    /**
     * Make the video play in a loop. This method could also be used to move to the next video in
     * a playlist.
     */
    @Override
    public void onCompletion() {
      videoWidgetView.seekTo(0);
    }
  }

  /**
   * Helper class to manage threading.
   */
  class VideoLoaderTask extends AsyncTask<Uri, Void, Boolean> {
    @Override
    protected Boolean doInBackground(Uri... uri) {
      try {
        if (uri == null || uri.length <1|| uri[0] == null) {
          videoWidgetView.loadVideoFromAsset("final_ar.mp4");
        } else {
          videoWidgetView.loadVideo(uri[0]);
        }
      } catch (IOException e) {
        // An error here is normally due to being unable to locate the file.
        loadVideoSuccessful = false;
        // Since this is a background thread, we need to switch to the main thread to show a toast.
        videoWidgetView.post(new Runnable() {
          @Override
          public void run() {
            Toast
                .makeText(SimpleVrVideoActivity.this, "Error opening file. ", Toast.LENGTH_LONG)
                .show();
          }
        });
        Log.e(TAG, "Could not open video: " + e);
      }

      return true;
    }
  }
}
