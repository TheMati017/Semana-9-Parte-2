package com.example.semana9;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_READ_IMAGES = 100;

    private ImageView imageView;
    private Button playAudioButton;
    private VideoView videoView;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageView = findViewById(R.id.imageView);
        playAudioButton = findViewById(R.id.playAudioButton);
        videoView = findViewById(R.id.myVideoView);

        configurarAnimacion();
        configurarAudio();
        configurarVideo();
        configurarGaleria();
    }

    // ---------- Animación ----------
    private void configurarAnimacion() {
        Button animateButton = findViewById(R.id.animateButton);
        animateButton.setOnClickListener(v -> {
            Animation fadeIn = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in);
            imageView.startAnimation(fadeIn);
        });
    }

    // ---------- Audio (MediaPlayer, res/raw) ----------
    private void configurarAudio() {
        playAudioButton.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                detenerAudio();
            } else {
                iniciarAudio();
            }
        });
    }

    private void iniciarAudio() {
        liberarAudio();
        mediaPlayer = MediaPlayer.create(this, R.raw.audio_demo);
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.setOnCompletionListener(mp -> {
            liberarAudio();
            playAudioButton.setText(R.string.btn_audio_play);
        });
        mediaPlayer.start();
        playAudioButton.setText(R.string.btn_audio_stop);
    }

    private void detenerAudio() {
        liberarAudio();
        playAudioButton.setText(R.string.btn_audio_play);
    }

    private void liberarAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // ---------- Video (VideoView, res/raw) ----------
    private void configurarVideo() {
        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoView);
        videoView.setMediaController(controller);

        Button playVideoButton = findViewById(R.id.playVideoButton);
        playVideoButton.setOnClickListener(v -> {
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_demo);
            videoView.setVideoURI(videoUri);
            videoView.start();
        });
    }

    // ---------- Permisos en tiempo de ejecución ----------
    private void configurarGaleria() {
        Button loadGalleryButton = findViewById(R.id.loadGalleryButton);
        loadGalleryButton.setOnClickListener(v -> {
            if (tienePermisoImagenes()) {
                cargarUltimaImagen();
            } else {
                ActivityCompat.requestPermissions(
                        this, new String[]{permisoImagenes()}, REQUEST_READ_IMAGES);
            }
        });
    }

    /** Android 13+ usa READ_MEDIA_IMAGES; versiones anteriores, READ_EXTERNAL_STORAGE. */
    private String permisoImagenes() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private boolean tienePermisoImagenes() {
        return ContextCompat.checkSelfPermission(this, permisoImagenes())
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_READ_IMAGES) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cargarUltimaImagen();
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permisoImagenes())) {
            Toast.makeText(this, R.string.msg_permiso_denegado_permanente, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.msg_permiso_denegado, Toast.LENGTH_SHORT).show();
        }
    }

    /** Muestra en el ImageView la foto más reciente de la galería. */
    private void cargarUltimaImagen() {
        String[] projection = {MediaStore.Images.Media._ID};
        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null,
                MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            if (cursor == null || !cursor.moveToFirst()) {
                Toast.makeText(this, R.string.msg_sin_imagenes, Toast.LENGTH_SHORT).show();
                return;
            }
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            imageView.setImageURI(uri);
            imageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        } catch (Exception e) {
            Toast.makeText(this, R.string.msg_error_imagen, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        liberarAudio();
    }
}
