package ntu.nguyentruong.smartrecipeapp;

import android.app.ProgressDialog;
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

import com.bumptech.glide.Glide; // Cần thêm thư viện Glide
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

    private Uri imageUri; // Uri ảnh mới chọn từ máy
    private String oldImageUrl; // Lưu link ảnh cũ (dùng khi edit)
    private ActivityResultLauncher<String> imagePickerLauncher;

    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    // Biến kiểm tra trạng thái Edit
    private boolean isEditMode = false;
    private String editingRecipeId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_foods2);

        initViews();
        initFirebase();
        initCloudinary();
        setupImagePicker();

        handleEvents();

        // Kiểm tra xem có phải đang sửa bài không
        if (getIntent().getBooleanExtra("IS_EDIT", false)) {
            isEditMode = true;
            editingRecipeId = getIntent().getStringExtra("RECIPE_ID");
            btnPublish.setText("Cập nhật & Gửi duyệt");
            loadRecipeToEdit(editingRecipeId);
        } else {
            // Nếu là tạo mới thì thêm sẵn 1 dòng trống cho đẹp
            addIngredientRow("", "");
            addStepRow("");
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
        progressDialog.setMessage("Đang xử lý dữ liệu...");
        progressDialog.setCancelable(false);
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void initCloudinary() {
        try {
            MediaManager.get();
        } catch (Exception e) {
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

        // Truyền rỗng để tạo dòng mới
        btnAddIngredientLine.setOnClickListener(v -> addIngredientRow("", ""));
        btnAddStepLine.setOnClickListener(v -> addStepRow(""));

        btnPublish.setOnClickListener(v -> validateAndSubmit());
    }

    // --- XỬ LÝ GIAO DIỆN ĐỘNG (Thêm tham số để hỗ trợ Edit) ---

    private void addIngredientRow(String nameVal, String quantityVal) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_add_ingredient, layoutIngredientsContainer, false);

        EditText edtName = view.findViewById(R.id.edtIngredientName);
        EditText edtQuant = view.findViewById(R.id.edtIngredientQuant);

        // Set dữ liệu nếu có (dùng cho Edit)
        edtName.setText(nameVal);
        edtQuant.setText(quantityVal);

        view.findViewById(R.id.btnRemove).setOnClickListener(v -> layoutIngredientsContainer.removeView(view));
        layoutIngredientsContainer.addView(view);
    }

    private void addStepRow(String stepContent) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_add_step, layoutStepsContainer, false);

        EditText edtContent = view.findViewById(R.id.edtStepContent);
        TextView tvIndex = view.findViewById(R.id.tvStepIndex);

        // Set dữ liệu nếu có
        edtContent.setText(stepContent);

        view.findViewById(R.id.btnStepRemove).setOnClickListener(v -> layoutStepsContainer.removeView(view));

        // Cập nhật lại số thứ tự (chỉ là hiển thị tạm, có thể làm kỹ hơn nếu muốn)
        if (tvIndex != null) {
            tvIndex.setText(String.valueOf(layoutStepsContainer.getChildCount() + 1));
        }

        layoutStepsContainer.addView(view);
    }

    // --- LOGIC VALIDATE & PHÂN LUỒNG UPLOAD ---

    private void validateAndSubmit() {
        String name = edtRecipeName.getText().toString().trim();
        String time = edtTime.getText().toString().trim();

        // Nếu tạo mới bắt buộc có ảnh. Nếu Edit thì có thể null (dùng ảnh cũ)
        if (!isEditMode && imageUri == null) {
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
            Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        // LOGIC QUAN TRỌNG:
        if (imageUri != null) {
            // 1. Nếu người dùng chọn ảnh mới -> Upload Cloudinary -> Lấy link mới -> Save
            uploadToCloudinary(name, time, ingredients, steps);
        } else {
            // 2. Nếu đang Edit và KHÔNG chọn ảnh mới -> Dùng link cũ -> Save luôn
            saveToFirestore(name, time, ingredients, steps, oldImageUrl);
        }
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
                // Format: Tên (Số lượng)
                list.add(ten + (luong.isEmpty() ? "" : " (" + luong + ")"));
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
            if (!step.isEmpty()) list.add(step);
        }
        return list;
    }

    private void uploadToCloudinary(String name, String time, List<String> ingredients, List<String> steps) {
        MediaManager.get().upload(imageUri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String newImageUrl = (String) resultData.get("secure_url");
                        // Upload xong thì lưu vào Firestore
                        saveToFirestore(name, time, ingredients, steps, newImageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progressDialog.dismiss();
                        Toast.makeText(AddFoodsActivity2.this, "Lỗi upload ảnh: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}
                }).dispatch();
    }

    // --- HÀM LƯU / CẬP NHẬT FIRESTORE ---
    private void saveToFirestore(String name, String time, List<String> ingredients, List<String> steps, String imageUrl) {
        String currentUserId = FirebaseAuth.getInstance().getUid();

        // Tạo object MonAn (hoặc Map)
        Map<String, Object> data = new HashMap<>();
        data.put("id", ""); // ID sẽ được update sau khi add thành công
        data.put("tenMon", name);
        data.put("thoiGian", time);
        data.put("khauPhan", edtServes.getText().toString());
        data.put("hinhAnh", imageUrl);
        data.put("nguyenLieu", ingredients);
        data.put("cachLam", steps);
        data.put("authorId", currentUserId);
        data.put("createdAt", System.currentTimeMillis());

        // --- QUAN TRỌNG NHẤT: LOGIC CHỜ DUYỆT ---
        data.put("status", "pending");   // Bắt buộc là "pending"
        data.put("isPublic", false);     // Ẩn khỏi trang chủ ngay lập tức

        // Lưu vào collection chung là "recipes"
        db.collection("recipes") // Đảm bảo tên collection chính xác là "recipes"
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    // Cập nhật lại ID cho document
                    documentReference.update("id", documentReference.getId());

                    progressDialog.dismiss();
                    // Thông báo rõ ràng
                    Toast.makeText(this, "Đã gửi món ăn! Vui lòng chờ Admin duyệt.", Toast.LENGTH_LONG).show();
                    finish(); // Đóng màn hình, KHÔNG ĐƯỢC tự ý add vào list hiển thị cục bộ
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Lỗi gửi dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // --- LOGIC LOAD DỮ LIỆU ĐỂ SỬA ---
    private void loadRecipeToEdit(String recipeId) {
        progressDialog.setMessage("Đang tải dữ liệu cũ...");
        progressDialog.show();

        db.collection("recipes").document(recipeId).get()
                .addOnSuccessListener(document -> {
                    progressDialog.dismiss();
                    MonAn mon = document.toObject(MonAn.class);
                    if (mon != null) {
                        // 1. Load thông tin cơ bản
                        edtRecipeName.setText(mon.getTenMon());

                        // Xử lý chuỗi thời gian (bỏ chữ "phút" để hiện số vào edittext)
                        String timeStr = mon.getThoiGian().replace("phút", "").trim();
                        edtTime.setText(timeStr);

                        String servesStr = mon.getKhauPhan().replace("người", "").trim();
                        edtServes.setText(servesStr);

                        // 2. Load ảnh cũ
                        oldImageUrl = mon.getHinhAnh();
                        Glide.with(this).load(oldImageUrl).into(imgRealPhoto);
                        imgRealPhoto.setVisibility(View.VISIBLE);
                        findViewById(R.id.imgRecipePreview).setVisibility(View.GONE);

                        // 3. Load Nguyên liệu (Phức tạp: Phải tách chuỗi "Tên (SL)" ra)
                        if (mon.getNguyenLieu() != null) {
                            for (String item : mon.getNguyenLieu()) {
                                // Giả sử format là "Thịt bò (500g)"
                                String name = item;
                                String quant = "";
                                if (item.contains("(") && item.endsWith(")")) {
                                    int index = item.lastIndexOf("(");
                                    name = item.substring(0, index).trim();
                                    quant = item.substring(index + 1, item.length() - 1);
                                }
                                addIngredientRow(name, quant);
                            }
                        }

                        // 4. Load Cách làm
                        if (mon.getCachLam() != null) {
                            for (String step : mon.getCachLam()) {
                                addStepRow(step);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Lỗi tải bài viết", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // Hàm phụ để tạo keyword tìm kiếm (Không bắt buộc)
    private List<String> generateKeywords(String name) {
        List<String> keywords = new ArrayList<>();
        String[] words = name.toLowerCase().split(" ");
        for (String word : words) {
            keywords.add(word);
        }
        keywords.add(name.toLowerCase());
        return keywords;
    }
}