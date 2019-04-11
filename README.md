
# reference:
 http://mussyu1204.myhome.cx/wordpress/it/?p=127

# change:
  ## 1. add button for start camera.

```java
            Button btn = findViewById(R.id.button2);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openCamera();
                }
            });
```

  ## 2. add permisson checek.

```java
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
 ```
