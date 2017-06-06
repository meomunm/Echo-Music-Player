package com.lunchareas.divertio.activities;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lunchareas.divertio.R;
import com.lunchareas.divertio.adapters.SongFixedAdapter;
import com.lunchareas.divertio.fragments.ChangeSongArtistDialog;
import com.lunchareas.divertio.fragments.ChangeSongTitleDialog;
import com.lunchareas.divertio.models.PlaylistDBHandler;
import com.lunchareas.divertio.models.PlaylistData;
import com.lunchareas.divertio.models.SongData;
import com.lunchareas.divertio.utils.PlaylistQueueUtil;
import com.lunchareas.divertio.utils.SongUtil;
//https://www.youtube.com/watch?v=1UlRIbpYTwk
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlaylistActivity extends BasePlayerActivity {

    private static final String TAG = PlaylistActivity.class.getName();

    private List<SongData> songInfoList;
    private ListView playlistView;
    private ImageView playButton;
    private ImageView playlistIcon;
    private PlaylistData playlistData;

    public PlaylistActivity() {
        super(R.layout.activity_playlist_manager);
    }

    @Override
    @SuppressLint("NewApi")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get playlist data
        if (getIntent() == null) {
            Log.e(TAG, "Cannot find intent?");
        }
        if (getIntent().getExtras() == null) {
            Log.e(TAG, "Extras were not passed to playlist manager.");
        }
        String playlistName = getIntent().getStringExtra(PlaylistMenuActivity.PLAYLIST_NAME);
        PlaylistDBHandler db = new PlaylistDBHandler(this);
        playlistData = db.getPlaylistData(playlistName);

        // Songs in playlist
        songInfoList = new ArrayList<>();
        playlistView = (ListView) findViewById(R.id.song_list);
        playlistIcon = (ImageView) findViewById(R.id.playlist_icon);
        setMainView();

        // Get play button
        playButton = (ImageView) findViewById(R.id.playlist_play_button);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"Playing playlist.");
                songCtrlButton.setBackgroundResource(R.drawable.pause);
                sendMusicPauseIntent();
                PlaylistQueueUtil queueUtil = new PlaylistQueueUtil(playlistData, PlaylistActivity.this);
                queueUtil.startQueue();
                musicBound = true;
            }
        });

        // Just for feeling
        playlistView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Detected LONG click on playlist song.");
                //showChoiceMenu(view, position);
                return true;
            }
        });
    }

    @Override
    protected void initSongbar() {

        // Setup song bar
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        songProgressManager = (SeekBar) findViewById(R.id.progress_bar);
        songProgressManager.getProgressDrawable().setColorFilter(getResources().getColor(R.color.red), PorterDuff.Mode.SRC_IN);
        songProgressManager.getThumb().setColorFilter(getResources().getColor(R.color.red), PorterDuff.Mode.SRC_IN);
        songCtrlButton = (ImageButton) findViewById(R.id.play_button);
        if (am.isMusicActive()) {
            musicBound = true;
            songCtrlButton.setBackgroundResource(R.drawable.pause);
        } else {
            musicBound = false;
            songCtrlButton.setBackgroundResource(R.drawable.play);
        }

        // Setup play button
        songCtrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Detected click on play button.");
                if (musicBound) {
                    sendMusicPauseIntent();
                    songCtrlButton.setBackgroundResource(R.drawable.play);
                    musicBound = false;
                } else {
                    sendMusicStartIntent();
                    songCtrlButton.setBackgroundResource(R.drawable.pause);
                    musicBound = true;
                }
            }
        });

        // Setup progress manager
        songProgressManager.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int position, boolean userPressed) {
                if (userPressed) {
                    sendMusicChangeIntent(position);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Prevents broken music during time change
                sendMusicPauseIntent();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Resumes regular music from pause
                sendMusicStartIntent();
                songCtrlButton.setBackgroundResource(R.drawable.pause);
            }
        });

        // Setup time manager
        songBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int songPosition = intent.getIntExtra(PlayMusicService.MUSIC_POSITION, 0);
                int songDuration = intent.getIntExtra(PlayMusicService.MUSIC_DURATION, 0);

                // Set location based on position/duration
                songProgressManager.setMax(songDuration);
                songProgressManager.setProgress(songPosition);

                // Set new text in time
                String songPositionTime = String.format(
                        Locale.US, "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(songPosition),
                        TimeUnit.MILLISECONDS.toSeconds(songPosition) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(songPosition))
                );

                String songDurationTime = String.format(
                        Locale.US, "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(songDuration),
                        TimeUnit.MILLISECONDS.toSeconds(songDuration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(songDuration))
                );

                String totalSongTime = songPositionTime + "/" + songDurationTime;
                TextView songTimeView = (TextView) findViewById(R.id.time_info);
                songTimeView.setText(totalSongTime);
            }
        };
    }

    @SuppressLint("NewApi")
    public void showChoiceMenu(View view, final int pos) {
        final PopupMenu popupMenu = new PopupMenu(context, view, Gravity.END);
        final SongData selectedSong = songInfoList.get(pos);

        // Handle individual clicks
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.song_rename_title: {
                        Log.d(TAG, "Renaming song title!");

                        // Create popup for new title
                        DialogFragment changeSongTitleDialog = new ChangeSongTitleDialog();
                        Bundle bundle = new Bundle();
                        bundle.putInt(ChangeSongTitleDialog.MUSIC_POS, pos);
                        changeSongTitleDialog.setArguments(bundle);
                        changeSongTitleDialog.show(getSupportFragmentManager(), "ChangeTitle");

                        return true;
                    }
                    case R.id.song_delete_title: {
                        Log.d(TAG, "Deleting song!");

                        // Remove song from list and re-update view
                        SongUtil songController = new SongUtil(context);
                        songController.deleteSong(selectedSong);
                        setMainView();

                        return true;
                    }
                    case R.id.song_change_artist: {
                        Log.d(TAG, "Changing song artist!");

                        // Create popup for new artist
                        DialogFragment changeSongArtistDialog = new ChangeSongArtistDialog();
                        Bundle bundle = new Bundle();
                        bundle.putInt(ChangeSongArtistDialog.MUSIC_POS, pos);
                        changeSongArtistDialog.setArguments(bundle);
                        changeSongArtistDialog.show(getSupportFragmentManager(), "ChangeArtist");

                        return true;
                    }
                    default: {
                        return false;
                    }
                }
            }
        });

        // Create menu and show
        MenuInflater menuInflater = popupMenu.getMenuInflater();
        menuInflater.inflate(R.menu.playlist_manager_choice_menu, popupMenu.getMenu());
        popupMenu.show();
    }

    @Override
    protected void setDisplay() {
        setContentView(R.layout.activity_playlist_manager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.playlist_manager_overflow_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "Detected that position " + item.getItemId() + " was selected.");
        switch (item.getItemId()) {
            /*
            case R.id.playlist_rename: {
                Log.d(TAG, "Starting new dialog - rename.");
                return true;
            } */
            case R.id.playlist_add: {
                Log.d(TAG, "Starting new dialog - add.");
                return true;
            }
            case R.id.playlist_delete: {
                Log.d(TAG, "Starting new dialog - delete.");
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Options for drawer menu
    @Override
    protected void selectMenuItem(int position) {
        Log.d(TAG, "Detected click on position " + position + ".");
        switch (position) {
            case 0: {
                Log.d(TAG, "Starting new activity - main!");
                Intent i = new Intent(this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                break;
            }
            case 1: {
                Log.d(TAG, "Starting new activity - playlist.");
                Intent i = new Intent(this, PlaylistMenuActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                break;
            }
            /*
            case 2: {
                Log.d(TAG, "Starting new activity - bluetooth.");
                Intent i = new Intent(this, BluetoothActivity.class);
                startActivity(i);
                break;
            }
            case 3: {
                Log.d(TAG, "Starting new activity - settings.");
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;
            }
            */
        }
    }

    @Override
    public void setMainView() {
        //cleanMusicFileDir();
        Log.d(TAG, "Resetting main view for playlist controller activity.");
        getSongsForActivity();
        SongFixedAdapter songListAdapter = new SongFixedAdapter(this, songInfoList);
        playlistView.setAdapter(songListAdapter);

        // Set center icon image
        if (playlistIcon != null) {
            if (playlistData.getPlaylistIcon() == null) {
                Log.d(TAG, "No default playlist icon found.");

                // Try with song icon
                List<SongData> songList = playlistData.getSongList();
                Collections.shuffle(songList);
                boolean found = false;
                for (SongData songData : songList) {
                    if (songData.getSongCover() != null) {
                        playlistIcon.setImageDrawable(songData.getSongCover());
                        found = true;
                    }
                }

                // Songs had no icon too
                if (!found) {
                    Drawable drawable = getResources().getDrawable(R.drawable.default_song_icon);
                    playlistIcon.setImageDrawable(drawable);
                }

            } else {
                playlistIcon.setImageDrawable(playlistData.getPlaylistIcon());
            }
        }
    }

    public void getSongsForActivity() {

        // Get database and song list
        songInfoList = playlistData.getSongList();
    }
}
