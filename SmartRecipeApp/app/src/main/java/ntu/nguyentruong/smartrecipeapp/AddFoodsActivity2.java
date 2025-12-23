package ntu.nguyentruong.smartrecipeapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddFoodsActivity2 extends AppCompatActivity {

    private ImageButton btnClose;
    private CardView cardUploadImage;
    private ImageView imgRealPhoto;
    private EditText edtRecipeName, edtTime, edtServes;
    private LinearLayout layoutIngredientsContainer, layoutStepsContainer;
    private MaterialButton btnAddIngredientLine, btnAddStepLine, btnPublish;

    private Uri imageUri;
    private ActivityResultLauncher<String> imagePickerLauncher;

    private FirebaseFirestore db;
    // Bỏ StorageReference của Firebase đi
    // private StorageReference storageRef;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_foods2);

        initViews();
        initFirebase();
        initCloudinary(); // <-- Cấu hình Cloudinary
        setupImagePicker();

        // Thêm sẵn dòng đầu tiên
        addIngredientRow();
        addStepRow();

        handleEvents();
        if (getIntent().getBooleanExtra("IS_EDIT", false)) {
            String recipeId = getIntent().getStringExtra("RECIPE_ID");
            loadRecipeToEdit(recipeId);
            btnPublish.setText("Cập nhật"); // Đổi tên nút
        }
    }

    private void initViews() {
        btnClose = findViewById(R.id.btnClose);
        cardUploadImage = findViewById(R.id.cardUploadImage);
        imgRealPhoto = findViewById(R.id.imgRealPhoto);
        edtRecipeName = findViewById(R.id.edtRecipeName);
        edtTime = findViewById(R.id.edtTime);
        edtServes = findViewById(R.id.edtServes);

        layoutIngredientsContainer = findViewById(R.id.layoutIngredientsContainer);
        layoutStepsContainer = findViewById(R.id.layoutStepsContainer);

        btnAddIngredientLine = findViewById(R.id.btnAddIngredientLine);
        btnAddStepLine = findViewById(R.id.btnAddStepLine);
        btnPublish = findViewById(R.id.btnPublish);

        layoutIngredientsContainer.removeAllViews();
        layoutStepsContainer.removeAllViews();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải món ăn lên...");
        progressDialog.setCancelable(false);
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    // --- CẤU HÌNH CLOUDINARY (QUAN TRỌNG) ---
    private void initCloudinary() {
        try {
            // Kiểm tra xem đã init chưa để tránh crash khi vào lại activity
            MediaManager.get();
        } catch (Exception e) {
            // Thay thế 3 dòng dưới bằng thông tin lấy từ Dashboard của Cloudinary
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dcy42u29w");
            config.put("api_key", "872117492863343");
            config.put("api_secret", "3LtfxWbmmO_QD9x_Spk4sCNf4N8");
            MediaManager.init(this, config);
        }
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imageUri = uri;
                        imgRealPhoto.setImageURI(uri);
                        imgRealPhoto.setVisibility(View.VISIBLE);
                        findViewById(R.id.imgRecipePreview).setVisibility(View.GONE);
                    }
                }
        );
    }

    private void handleEvents() {
        btnClose.setOnClickListener(v -> finish());
        cardUploadImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnAddIngredientLine.setOnClickListener(v -> addIngredientRow());
        btnAddStepLine.setOnClickListener(v -> addStepRow());
        btnPublish.setOnClickListener(v -> validateAndUpload());
    }

    // --- XỬ LÝ GIAO DIỆN ĐỘNG ---

    private void addIngredientRow() {
        View view = LayoutInflater.from(this).inflate(R.layout.item_add_ingredient, layoutIngredientsContainer, false);
        view.findViewById(R.id.btnRemove).setOnClickListener(v -> layoutIngredientsContainer.removeView(view));
        layoutIngredientsContainer.addView(view);
    }

    private void addStepRow() {
        View view = LayoutInflater.from(this).inflate(R.layout.item_add_step, layoutStepsContainer, false);
        TextView tvIndex = view.findViewById(R.id.tvStepIndex);
        view.findViewById(R.id.btnStepRemove).setOnClickListener(v -> layoutStepsContainer.removeView(view));
        if (tvIndex != null) {
            tvIndex.setText(String.valueOf(layoutStepsContainer.getChildCount() + 1));
        }

        layoutStepsContainer.addView(view);
    }

    // --- XỬ LÝ UPLOAD VỚI CLOUDINARY ---

    private void validateAndUpload() {
        String name = edtRecipeName.getText().toString().trim();
        String time = edtTime.getText().toString().trim();

        if (imageUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh món ăn", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên và thời gian", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> ingredients = getIngredientsFromLayout();
        List<String> steps = getStepsFromLayout();

        if (ingredients.isEmpty() || steps.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập ít nhất 1 nguyên liệu và 1 bước làm", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        uploadToCloudinary(name, time, ingredients, steps);
    }

    private List<String> getIngredientsFromLayout() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < layoutIngredientsContainer.getChildCount(); i++) {
            View view = layoutIngredientsContainer.getChildAt(i);
            EditText edtName = view.findViewById(R.id.edtIngredientName);
            EditText edtQuant = view.findViewById(R.id.edtIngredientQuant);

            String ten = edtName.getText().toString().trim();
            String luong = edtQuant.getText().toString().trim();

            if (!ten.isEmpty()) {
                list.add(ten + " (" + luong + ")");
            }
        }
        return list;
    }

    private List<String> getStepsFromLayout() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < layoutStepsContainer.getChildCount(); i++) {
            View view = layoutStepsContainer.getChildAt(i);
            EditText edtContent = view.findViewById(R.id.edtStepContent);

            String step = edtContent.getText().toString().trim();
            if (!step.isEmpty()) {
                list.add(step);
            }
        }
        return list;
    }

    // --- HÀM UPLOAD ẢNH LÊN CLOUDINARY ---
    private void uploadToCloudinary(String name, String time, List<String> ingredients, List<String> steps) {

        MediaManager.get().upload(imageUri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        // Bắt đầu upload
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Có thể cập nhật thanh progress nếu muốn
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // Upload thành công -> Lấy link ảnh HTTPS
                        String imageUrl = (String) resultData.get("secure_url");

                        // Sau khi có link ảnh, lưu thông tin vào Firestore
                        saveToFirestore(name, time, ingredients, steps, imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progressDialog.dismiss();
                        Toast.makeText(AddFoodsActivity2.this, "Lỗi Cloudinary: " + error.getDescription(), Toast.LENGTH_LONG).show();
                        Log.e("UPLOAD_ERR", error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Upload bị hoãn lại do mạng yếu...
                    }
                })
                .dispatch();
    }

    private void saveToFirestore(String name, String time, List<String> ingredients, List<String> steps, String imageUrl) {
        String currentUserId = FirebaseAuth.getInstance().getUid();

        MonAn monAn = new MonAn();
        monAn.setTenMon(name);
        monAn.setThoiGian(time + " phút");
        monAn.setKhauPhan(edtServes.getText().toString() + " người");
        monAn.setHinhAnh(imageUrl); // Link ảnh từ Cloudinary
        monAn.setNguyenLieu(ingredients);
        monAn.setCachLam(steps);
        monAn.setAuthorId(currentUserId);
        monAn.setCreatedAt(System.currentTimeMillis());
        monAn.setStatus("pending"); // Chờ duyệt

        db.collection("recipes")
                .add(monAn)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    documentReference.update("id", docId);

                    progressDialog.dismiss();
                    Toast.makeText(this, "Gửi thành công! Vui lòng chờ Admin duyệt.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void loadRecipeToEdit(String recipeId) {
        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(document -> {
                    MonAn mon = document.toObject(MonAn.class);
                    if (mon != null) {
                        edtRecipeName.setText(mon.getTenMon());
                        // ... set text cho các trường khác ...
                        // Logic hiển thị lại nguyên liệu/cách làm hơi phức tạp (cần loop để addView)
                        // Nếu ảnh cũ có, dùng Glide load vào imgRealPhoto
                    }
                });
    }
}