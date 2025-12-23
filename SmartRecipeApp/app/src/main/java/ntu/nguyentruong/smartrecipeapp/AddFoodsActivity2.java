package ntu.nguyentruong.smartrecipeapp;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddFoodsActivity2 extends AppCompatActivity {

    private ImageButton btnClose;
    private CardView cardUploadImage;
    private ImageView imgRealPhoto;
    private EditText edtRecipeName, edtTime, edtServes;
    private LinearLayout layoutIngredientsContainer, layoutStepsContainer;
    private MaterialButton btnAddIngredientLine, btnAddStepLine, btnPublish;

    private Uri imageUri; // Lưu đường dẫn ảnh đã chọn
    private ActivityResultLauncher<String> imagePickerLauncher;

    private FirebaseFirestore db;
    private StorageReference storageRef;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_foods2);

        initViews();
        initFirebase();
        setupImagePicker();

        // Thêm sẵn 1 dòng nhập liệu đầu tiên cho đẹp
        addIngredientRow();
        addStepRow();

        handleEvents();
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

        // Nhớ xóa các view mẫu (EditText) bạn viết cứng trong XML chính
        // để tránh bị duplicate ID hoặc xử lý sai.
        // Trong XML chính, layoutIngredientsContainer nên rỗng.
        layoutIngredientsContainer.removeAllViews();
        layoutStepsContainer.removeAllViews();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải món ăn lên...");
        progressDialog.setCancelable(false);
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imageUri = uri;
                        imgRealPhoto.setImageURI(uri);
                        imgRealPhoto.setVisibility(View.VISIBLE);
                        // Ẩn cái icon camera đi
                        findViewById(R.id.imgRecipePreview).setVisibility(View.GONE);
                    }
                }
        );
    }

    private void handleEvents() {
        btnClose.setOnClickListener(v -> finish());

        // Chọn ảnh
        cardUploadImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Thêm dòng nguyên liệu
        btnAddIngredientLine.setOnClickListener(v -> addIngredientRow());

        // Thêm dòng cách làm
        btnAddStepLine.setOnClickListener(v -> addStepRow());

        // Nút Đăng tải
        btnPublish.setOnClickListener(v -> validateAndUpload());
    }

    // --- XỬ LÝ GIAO DIỆN ĐỘNG ---

    private void addIngredientRow() {
        View view = LayoutInflater.from(this).inflate(R.layout.item_add_ingredient, layoutIngredientsContainer, false);

        // Xử lý nút xóa dòng
        view.findViewById(R.id.btnRemove).setOnClickListener(v -> {
            layoutIngredientsContainer.removeView(view);
        });

        layoutIngredientsContainer.addView(view);
    }

    private void addStepRow() {
        View view = LayoutInflater.from(this).inflate(R.layout.item_add_step, layoutStepsContainer, false);

        // Tự động đánh số bước (1, 2, 3...)
        TextView tvIndex = view.findViewById(R.id.tvIndex);
        tvIndex.setText(String.valueOf(layoutStepsContainer.getChildCount() + 1));

        layoutStepsContainer.addView(view);
    }

    // --- XỬ LÝ UPLOAD DỮ LIỆU ---

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

        // Lấy dữ liệu từ các dòng động
        List<String> ingredients = getIngredientsFromLayout();
        List<String> steps = getStepsFromLayout();

        if (ingredients.isEmpty() || steps.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập ít nhất 1 nguyên liệu và 1 bước làm", Toast.LENGTH_SHORT).show();
            return;
        }

        // Bắt đầu upload
        progressDialog.show();
        uploadImageAndSaveRecipe(name, time, ingredients, steps);
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
                list.add(ten + " (" + luong + ")"); // Lưu dạng: Gà (500g)
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

    private void uploadImageAndSaveRecipe(String name, String time, List<String> ingredients, List<String> steps) {
        // Tên file ảnh unique
        String fileName = UUID.randomUUID().toString();
        StorageReference imgRef = storageRef.child("recipe_images/" + fileName);

        imgRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Upload ảnh thành công -> Lấy URL
                    imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        saveToFirestore(name, time, ingredients, steps, imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToFirestore(String name, String time, List<String> ingredients, List<String> steps, String imageUrl) {
        String currentUserId = FirebaseAuth.getInstance().getUid();

        MonAn monAn = new MonAn();
        monAn.setTenMon(name);
        monAn.setThoiGian(time + " phút");
        monAn.setKhauPhan(edtServes.getText().toString() + " người");
        monAn.setHinhAnh(imageUrl);
        monAn.setNguyenLieu(ingredients);
        monAn.setCachLam(steps);
        monAn.setAuthorId(currentUserId);
        monAn.setCreatedAt(System.currentTimeMillis());

        // QUAN TRỌNG: Set status là PENDING để chờ admin duyệt
        monAn.setStatus("pending");

        // Lưu vào collection "MonAn" (hoặc "recipes" - nhớ thống nhất tên)
        db.collection("recipes") // Hoặc Constants.COLLECTION_MON_AN
                .add(monAn)
                .addOnSuccessListener(documentReference -> {
                    // Update lại ID cho document vừa tạo
                    String docId = documentReference.getId();
                    documentReference.update("id", docId);

                    progressDialog.dismiss();
                    Toast.makeText(this, "Gửi thành công! Vui lòng chờ Admin duyệt.", Toast.LENGTH_LONG).show();
                    finish(); // Đóng màn hình
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}