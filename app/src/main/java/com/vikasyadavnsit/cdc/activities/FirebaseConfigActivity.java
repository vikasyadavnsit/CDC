package com.vikasyadavnsit.cdc.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.enums.UserRole;
import com.vikasyadavnsit.cdc.utils.FirebaseConfigValidator;
import com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class FirebaseConfigActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ROLE = "extra_user_role";

    private EditText etConfigJson;
    private TextView tvStatus;
    private Button btnValidate, btnSelectFile;
    private MaterialButton btnTestConnection, btnTestWrite, btnProceed;
    private View layoutTesting;
    private String validatedJson = null;
    private String referenceJson = null;

    private boolean connectionVerified = false;
    private boolean writeVerified = false;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        readJsonFromUri(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_config);

        etConfigJson = findViewById(R.id.et_config_json);
        tvStatus = findViewById(R.id.tv_status);
        btnValidate = findViewById(R.id.btn_validate);
        btnSelectFile = findViewById(R.id.btn_select_file);
        
        layoutTesting = findViewById(R.id.layout_testing);
        btnTestConnection = findViewById(R.id.btn_test_connection);
        btnTestWrite = findViewById(R.id.btn_test_write);
        btnProceed = findViewById(R.id.btn_proceed);

        loadReferenceJson();

        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/json");
            filePickerLauncher.launch(intent);
        });

        btnValidate.setOnClickListener(v -> validateContent(etConfigJson.getText().toString()));
        btnTestConnection.setOnClickListener(v -> testConnection());
        btnTestWrite.setOnClickListener(v -> testWritePermissions());
        btnProceed.setOnClickListener(v -> saveAndProceed());
    }

    private void loadReferenceJson() {
        try (InputStream is = getAssets().open("google-services-reference.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            referenceJson = reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readJsonFromUri(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String content = reader.lines().collect(Collectors.joining("\n"));
            etConfigJson.setText(content);
            validateContent(content);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateContent(String json) {
        com.vikasyadavnsit.cdc.utils.LoggerUtils.d("FirebaseConfigActivity", "Validating Firebase JSON content");
        if (json == null || json.isEmpty()) {
            setStatus("Please provide JSON content", false);
            return;
        }

        if (com.vikasyadavnsit.cdc.utils.FirebaseConfigValidator.containsInjection(json)) {
            com.vikasyadavnsit.cdc.utils.LoggerUtils.w("FirebaseConfigActivity", "Potential injection detected in JSON!");
            setStatus("Security warning: Potential injection detected!", false);
            return;
        }

        String jsonPackage = com.vikasyadavnsit.cdc.utils.FirebaseConfigValidator.getPackageNameFromJson(json);
        String appPackage = getPackageName();
        if (jsonPackage != null && !jsonPackage.equalsIgnoreCase(appPackage)) {
            com.vikasyadavnsit.cdc.utils.LoggerUtils.e("FirebaseConfigActivity", "Package mismatch! JSON: " + jsonPackage + ", App: " + appPackage);
            setStatus("CRITICAL ERROR: This JSON is for " + jsonPackage + " but this app is " + appPackage + ". Re-download correct file.", false);
            validatedJson = null;
            layoutTesting.setVisibility(View.GONE);
            return;
        }

        if (com.vikasyadavnsit.cdc.utils.FirebaseConfigValidator.isValidStructure(json)) {
            if (referenceJson != null && !com.vikasyadavnsit.cdc.utils.FirebaseConfigValidator.validateAgainstReference(json, referenceJson)) {
                com.vikasyadavnsit.cdc.utils.LoggerUtils.w("FirebaseConfigActivity", "JSON structure does not match reference schema");
                setStatus("Structure does not match the required schema", false);
                return;
            }
            validatedJson = json;
            com.vikasyadavnsit.cdc.utils.LoggerUtils.i("FirebaseConfigActivity", "JSON validation successful");
            setStatus("JSON is valid. Proceed with verification steps below.", true);
            layoutTesting.setVisibility(View.VISIBLE);
            resetTests();
        } else {
            validatedJson = null;
            com.vikasyadavnsit.cdc.utils.LoggerUtils.e("FirebaseConfigActivity", "Invalid google-services.json structure");
            setStatus("Invalid google-services.json structure", false);
            layoutTesting.setVisibility(View.GONE);
        }
    }

    private void resetTests() {
        connectionVerified = false;
        writeVerified = false;
        btnTestConnection.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.surface_variant)));
        btnTestWrite.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.surface_variant)));
        btnTestWrite.setEnabled(false);
        btnProceed.setVisibility(View.GONE);
    }

    private void setStatus(String message, boolean success) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Status: " + message);
        tvStatus.setTextColor(success ? getColor(R.color.spending_credit) : getColor(R.color.spending_debit));
    }

    private void testConnection() {
        if (validatedJson == null) return;
        com.vikasyadavnsit.cdc.utils.LoggerUtils.i("FirebaseConfigActivity", "Testing RTDB connection...");
        setStatus("Verifying connection...", true);

        performFirebaseAction((tempApp, db) -> {
            // Changed from '.info/connected' to a standard path 'connectivity_test'
            // to avoid 'invalid token in path' errors caused by reserved keywords.
            db.getReference("connectivity_test").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() || (task.getException() != null && task.getException().getMessage().contains("Permission denied"))) {
                    connectionVerified = true;
                    com.vikasyadavnsit.cdc.utils.LoggerUtils.i("FirebaseConfigActivity", "RTDB Connection verified successfully");
                    btnTestConnection.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.spending_credit)));
                    btnTestConnection.setIcon(androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.ic_sync)); 
                    btnTestWrite.setEnabled(true);
                    setStatus("Connection verified! Now test write permissions.", true);
                } else {
                    String error = task.getException() != null ? task.getException().getMessage() : "Unknown";
                    com.vikasyadavnsit.cdc.utils.LoggerUtils.e("FirebaseConfigActivity", "RTDB Connection test failed: " + error);
                    setStatus("Connection failed: " + error, false);
                }
                tempApp.delete();
            });
        });
    }

    private void testWritePermissions() {
        if (!connectionVerified) return;
        com.vikasyadavnsit.cdc.utils.LoggerUtils.i("FirebaseConfigActivity", "Testing RTDB write/delete cycle...");
        setStatus("Testing write/delete cycle...", true);

        performFirebaseAction((tempApp, db) -> {
            // Use push() to create a unique node under a 'connection_tests' root
            DatabaseReference testRef = db.getReference("connection_tests").push();
            long timestamp = System.currentTimeMillis();
            
            // Try Write
            testRef.setValue(timestamp).addOnCompleteListener(writeTask -> {
                if (writeTask.isSuccessful()) {
                    com.vikasyadavnsit.cdc.utils.LoggerUtils.d("FirebaseConfigActivity", "Write test successful, attempting delete...");
                    // Try Delete (Cleanup)
                    testRef.removeValue().addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            writeVerified = true;
                            com.vikasyadavnsit.cdc.utils.LoggerUtils.i("FirebaseConfigActivity", "Write/Delete test successful");
                            btnTestWrite.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.spending_credit)));
                            btnProceed.setVisibility(View.VISIBLE);
                            setStatus("Verification complete! You can now proceed.", true);
                        } else {
                            com.vikasyadavnsit.cdc.utils.LoggerUtils.w("FirebaseConfigActivity", "Write successful, but delete failed. Check Rules.");
                            setStatus("Write successful, but Cleanup failed. Check Rules.", false);
                        }
                        tempApp.delete();
                    });
                } else {
                    String error = writeTask.getException() != null ? writeTask.getException().getMessage() : "Unknown";
                    com.vikasyadavnsit.cdc.utils.LoggerUtils.e("FirebaseConfigActivity", "Write test failed: " + error);
                    setStatus("Write failed! Ensure rules allow read/write.", false);
                    tempApp.delete();
                }
            });
        });
    }

    private interface FirebaseAction {
        void run(FirebaseApp app, FirebaseDatabase db);
    }

    private void performFirebaseAction(FirebaseAction action) {
        try {
            JsonObject root = JsonParser.parseString(validatedJson).getAsJsonObject();
            JsonObject projectInfo = root.getAsJsonObject("project_info");
            JsonObject client = root.getAsJsonArray("client").get(0).getAsJsonObject();
            String apiKey = client.getAsJsonArray("api_key").get(0).getAsJsonObject().get("current_key").getAsString();
            String appId = client.getAsJsonObject("client_info").get("mobilesdk_app_id").getAsString();
            String dbUrl = projectInfo.get("firebase_url").getAsString();
            String projectId = projectInfo.get("project_id").getAsString();

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId(appId)
                    .setApiKey(apiKey)
                    .setDatabaseUrl(dbUrl)
                    .setProjectId(projectId)
                    .build();

            String tempAppName = "temp_test_" + System.currentTimeMillis();
            FirebaseApp tempApp = FirebaseApp.initializeApp(this, options, tempAppName);
            FirebaseDatabase db = FirebaseDatabase.getInstance(tempApp);
            action.run(tempApp, db);
        } catch (Exception e) {
            setStatus("Initialization error: " + e.getMessage(), false);
        }
    }

    private void saveAndProceed() {
        if (!connectionVerified || !writeVerified) {
            Toast.makeText(this, "Please complete all verification steps first", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferenceUtils.saveFirebaseConfig(this, validatedJson);
        
        // Re-initialize Firebase with the new config immediately
        if (getApplication() instanceof com.vikasyadavnsit.cdc.MyApplication) {
            ((com.vikasyadavnsit.cdc.MyApplication) getApplication()).initializeFirebase();
        }

        UserRole selectedRole = (UserRole) getIntent().getSerializableExtra(EXTRA_USER_ROLE);
        SharedPreferenceUtils.setUserRole(this, selectedRole != null ? selectedRole : UserRole.CLIENT);
        SharedPreferenceUtils.setFirstLaunchCompleted(this);
        
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
