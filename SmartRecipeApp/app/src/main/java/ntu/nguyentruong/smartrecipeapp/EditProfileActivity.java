package ntu.nguyentruong.smartrecipeapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    EditText edtFullName, edtBio;
    Button btnSave;
    FirebaseFirestore db;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        edtFullName = findViewById(R.id.edtFullName);
        edtBio = findViewById(R.id.edtBio);
        btnSave = findViewById(R.id.btnSaveProfile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadCurrentInfo();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadCurrentInfo() {
        String uid = mAuth.getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if(doc.exists()) {
                User user = doc.toObject(User.class);
                edtFullName.setText(user.getFullName());
                edtBio.setText(user.getBio());
            }
        });
    }

    private void saveProfile() {
        String name = edtFullName.getText().toString();
        String bio = edtBio.getText().toString();
        String uid = mAuth.getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("bio", bio);

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Quay về trang trước
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}