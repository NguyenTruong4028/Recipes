package ntu.nguyentruong.recipesadmin;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class DetailRecipesActivity extends AppCompatActivity {

    private ImageView imgDetail;
    private TextView tvName, tvInfo, tvIngredients, tvSteps;
    private MaterialButton btnApprove, btnReject;

    private FirebaseFirestore db;
    private String recipeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_recipes);

        db = FirebaseFirestore.getInstance();
        recipeId = getIntent().getStringExtra("RECIPE_ID");

        initViews();
        loadData();
        setupActions();
    }

    private void initViews() {
        imgDetail = findViewById(R.id.imgDetailFood);
        tvName = findViewById(R.id.tvDetailName);
        tvInfo = findViewById(R.id.tvDetailInfo);
        tvIngredients = findViewById(R.id.tvIngredientsList);
        tvSteps = findViewById(R.id.tvStepsList);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
    }

    private void loadData() {
        if (recipeId == null) return;

        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(document -> {
                    MonAn mon = document.toObject(MonAn.class);
                    if (mon != null) {
                        tvName.setText(mon.getTenMon());
                        tvInfo.setText(mon.getThoiGian() + " • " + mon.getKhauPhan());

                        Glide.with(this).load(mon.getHinhAnh()).into(imgDetail);

                        // Hiển thị danh sách nguyên liệu (Convert List -> String)
                        StringBuilder ingBuilder = new StringBuilder();
                        if (mon.getNguyenLieu() != null) {
                            for (String item : mon.getNguyenLieu()) {
                                ingBuilder.append("• ").append(item).append("\n");
                            }
                        }
                        tvIngredients.setText(ingBuilder.toString());

                        // Hiển thị danh sách các bước
                        StringBuilder stepBuilder = new StringBuilder();
                        if (mon.getCachLam() != null) {
                            int count = 1;
                            for (String step : mon.getCachLam()) {
                                stepBuilder.append("Bước ").append(count++).append(": ")
                                        .append(step).append("\n\n");
                            }
                        }
                        tvSteps.setText(stepBuilder.toString());
                    }
                });
    }

    private void setupActions() {
        // Nút Duyệt
        btnApprove.setOnClickListener(v -> {
            db.collection("recipes").document(recipeId)
                    .update("status", "approved")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã duyệt bài!", Toast.LENGTH_SHORT).show();
                        finish(); // Quay lại dashboard
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // Nút Từ chối
        btnReject.setOnClickListener(v -> {
            db.collection("recipes").document(recipeId)
                    .update("status", "rejected") // Hoặc dùng .delete() nếu muốn xóa luôn
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã từ chối bài viết!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}