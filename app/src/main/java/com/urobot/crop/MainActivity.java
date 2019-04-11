package com.urobot.crop;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraMetadata;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;




public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 0;
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    private CameraDevice backCameraDevice;
    private CameraCaptureSession backCameraSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.button2);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });
    }
    private void requestCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            //new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
            // 追加説明が必要な場合の対応（サンプルではトーストを表示している）

        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CAMERA) {
            return;
        }
        // カメラパーミッションの使用が許可された
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // カメラの表示
            return;
        }
    }

    protected  void openCamera(){

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try {
            //カメラIDを取得（背面カメラを選択）
            String backCameraId = null;
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics chars
                        = manager.getCameraCharacteristics(cameraId);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);

                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    backCameraId = cameraId;
                }
            }

            // SurfaceViewにプレビューサイズを設定する(サンプルなので適当な値です)
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
            surfaceView.getHolder().setFixedSize(640, 320);


            // カメラオープン(オープンに成功したときに第2引数のコールバッククラスが呼ばれる)
            manager.openCamera(backCameraId, new OpenCameraCallback(), null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //ホームボタンを押した際などにカメラを開放する（開放しないと裏で動き続ける・・・）
    @Override
    protected void onPause() {
        super.onPause();
        // カメラセッションの終了
        if (backCameraSession != null) {
            try {
                backCameraSession.stopRepeating();
            } catch (CameraAccessException e) {
                Log.e(TAG, e.toString());
            }
            backCameraSession.close();
        }

        // カメラデバイスとの切断
        if (backCameraDevice != null) {
            backCameraDevice.close();
        }
    }

    //------------------------------------------------------------------------------------------------------------
    /**
     * カメラデバイス接続完了後のコールバッククラス
     */
    private class OpenCameraCallback extends CameraDevice.StateCallback {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            backCameraDevice = cameraDevice;

            // プレビュー用のSurfaceViewをリストに登録
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
            ArrayList<Surface> surfaceList = new ArrayList();
            surfaceList.add(surfaceView.getHolder().getSurface());

            try {
                // プレビューリクエストの設定（SurfaceViewをターゲットに）
                mPreviewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewRequestBuilder.addTarget(surfaceView.getHolder().getSurface());

                // キャプチャーセッションの開始(セッション開始後に第2引数のコールバッククラスが呼ばれる)
                cameraDevice.createCaptureSession(surfaceList, new CameraCaptureSessionCallback(), null);

            } catch (CameraAccessException e) {
                // エラー時の処理を記載
            }
        }



        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            // 切断時の処理を記載
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            // エラー時の処理を記載
        }
    }

    //------------------------------------------------------------------------------------------------------------
    /**
     * カメラが起動し使える状態になったら呼ばれるコールバック
     */
    private class CameraCaptureSessionCallback extends CameraCaptureSession.StateCallback {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            backCameraSession = session;

            try {
                // オートフォーカスの設定
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);

                // プレビューの開始(撮影時に第2引数のコールバッククラスが呼ばれる)
                session.setRepeatingRequest(mPreviewRequestBuilder.build(), new CaptureCallback(), null);

            } catch (CameraAccessException e) {
                Log.e(TAG, e.toString());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            //失敗時の処理を記載
        }
    }

    /**
     * カメラ撮影時に呼ばれるコールバック関数
     */
    private class CaptureCallback extends CameraCaptureSession.CaptureCallback {

    }
}
