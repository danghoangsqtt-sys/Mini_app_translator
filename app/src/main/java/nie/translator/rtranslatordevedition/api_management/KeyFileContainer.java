/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copyFile of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nie.translator.rtranslatordevedition.api_management;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import nie.translator.rtranslatordevedition.GeneralActivity;
import nie.translator.rtranslatordevedition.Global;
import nie.translator.rtranslatordevedition.R;
import nie.translator.rtranslatordevedition.tools.gui.KeyFileSelectorButton;

public class KeyFileContainer {
    private static final int REQUEST_CODE_PICK_CREDENTIAL = 6;
    private GeneralActivity activity;
    private AppCompatImageButton deleteButton;
    private Fragment fragment;
    private Global global;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private KeyFileSelectorButton selectFileButton;
    private TextView textView;

    public KeyFileContainer(final Global global, @NonNull final GeneralActivity activity, TextView textView, AppCompatImageButton deleteButton, KeyFileSelectorButton selectFileButton, Fragment fragment) {
        this.global = global;
        this.activity = activity;
        this.fragment = fragment;
        this.textView = textView;
        this.deleteButton = deleteButton;
        this.selectFileButton = selectFileButton;
        String apiKeyFileName = global.getApiKeyFileName();
        if (apiKeyFileName.length() > 0) {
            this.deleteButton.setVisibility(View.VISIBLE);
            this.textView.setText(apiKeyFileName);
        }
        this.deleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                activity.showConfirmDeleteDialog(new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        delete(new FileOperationListener() {
                            public void onSuccess() {
                                KeyFileContainer.this.textView.setText(global.getApiKeyFileName());
                                KeyFileContainer.this.deleteButton.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                });
            }
        });
        this.selectFileButton.setOnClickListenerForActivated(new View.OnClickListener() {
            public void onClick(View v) {
                openDocumentPicker();
            }
        });
    }

    private void openDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        fragment.startActivityForResult(intent, REQUEST_CODE_PICK_CREDENTIAL);
    }

    public void onActivityResult(int requestCode, Intent data) {
        if (requestCode != REQUEST_CODE_PICK_CREDENTIAL || data == null || data.getData() == null) {
            return;
        }
        save(data.getData(), new FileOperationListener() {
            @Override
            public void onSuccess() {
                KeyFileContainer.this.textView.setText(global.getApiKeyFileName());
                KeyFileContainer.this.deleteButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onInvalidCredential() {
                Toast.makeText(global, R.string.error_invalid_key, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure() {
                Toast.makeText(global, R.string.error_picking_file, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void save(final Uri uri, final FileOperationListener responseListener) {
        new Thread() {
            public void run() {
                try {
                    String displayName = getDisplayName(uri);
                    try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
                        if (input == null) {
                            throw new FileNotFoundException("Selected credential is unavailable");
                        }
                        global.getCredentialStore().importCredential(input);
                    }
                    global.setApiKeyFileName(displayName);
                    global.resetApiToken();
                    mainHandler.post(new Runnable() {
                        public void run() {
                            responseListener.onSuccess();
                        }
                    });
                } catch (final ServiceAccountCredentialValidator.InvalidCredentialException ignored) {
                    mainHandler.post(new Runnable() {
                        public void run() {
                            responseListener.onInvalidCredential();
                        }
                    });
                } catch (IOException ignored) {
                    mainHandler.post(new Runnable() {
                        public void run() {
                            responseListener.onFailure();
                        }
                    });
                }
            }
        }.start();
    }

    private String getDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = activity.getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int displayNameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (displayNameColumn >= 0 && !cursor.isNull(displayNameColumn)) {
                    String displayName = cursor.getString(displayNameColumn);
                    if (displayName != null && displayName.trim().length() > 0) {
                        return displayName;
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "service-account.json";
    }

    private void delete(final FileOperationListener responseListener) {
        new Thread() {
            public void run() {
                super.run();
                File legacyFile = new File(global.getFilesDir(), global.getApiKeyFileName());
                try {
                    global.getCredentialStore().deleteCredential(legacyFile);
                    global.setApiKeyFileName("");
                    global.resetApiToken();
                    mainHandler.post(new Runnable() {
                        public void run() {
                            responseListener.onSuccess();
                        }
                    });
                } catch (IOException ignored) {
                    mainHandler.post(new Runnable() {
                        public void run() {
                            responseListener.onFailure();
                        }
                    });
                }
            }
        }.start();
    }

    public static abstract class FileOperationListener {
        public void onSuccess() {
        }

        public void onInvalidCredential() {
        }

        public void onFailure() {
        }
    }
}
