package ntu.nguyentruong.smartrecipeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import java.util.List;

public class DetailActivity extends AppCompatActivity {
    private ImageView imgDetailFood;
    private TextView tvDetailName, tvDetailTime,tvDetailServe;
    private LinearLayout layoutIngredientsList, layoutStepsList;
    private ImageButton btnBackDetail, btnSaveFavorite;
    private MonAn monAnHienTai;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        initViews();
        getDataFromIntent();

        if (monAnHienTai != null) {
            setupUI();
            handleEvents();
        } else {
            Toast.makeText(this, "Không tìm thấy dữ liệu món ăn", Toast.LENGTH_SHORT).show();
            finish();
        }

    }
    private void initViews() {
        imgDetailFood = findViewById(R.id.imgDetailFood);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailTime = findViewById(R.id.tvDetailTime);
        tvDetailServe = findViewById(R.id.tvDetailServe);

        layoutIngredientsList = findViewById(R.id.layoutIngredientsList);
        layoutStepsList = findViewById(R.id.layoutStepsList);

        btnBackDetail = findViewById(R.id.btnBackDetail);
        btnSaveFavorite = findViewById(R.id.btnSaveFavorite);
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        // Lấy object MonAn được truyền sang.
        // Key "object_monan" phải khớp với key bên Activity gửi (Danh sách)
        if (intent != null && intent.hasExtra("object_monan")) {
            monAnHienTai = (MonAn) intent.getSerializableExtra("object_monan");
        }
    }

    private void setupUI() {
        // 1. Hiển thị thông tin cơ bản
        tvDetailName.setText(monAnHienTai.getTenMon());
        tvDetailTime.setText("⏱ " + monAnHienTai.getThoiGian());

        if(tvDetailServe != null) {
            tvDetailServe.setText("👥 " + monAnHienTai.getKhauPhan());
        }

        // 2. Load ảnh từ URL bằng Glide
        Glide.with(this)
                .load(monAnHienTai.getHinhAnh())
                .placeholder(R.drawable.bg_rounded_pink) // Hình hiển thị khi đang load
                .error(R.drawable.bg_rounded_pink)       // Hình hiển thị khi lỗi
                .into(imgDetailFood);

        // 3. Xử lý danh sách Nguyên liệu (Dynamic CheckBox)
        layoutIngredientsList.removeAllViews(); // Xóa view mẫu trong XML
        List<String> nguyenLieus = monAnHienTai.getNguyenLieu();

        if (nguyenLieus != null) {
            for (String item : nguyenLieus) {
                CheckBox checkBox = new CheckBox(this);
                checkBox.setText(item);
                checkBox.setTextColor(getResources().getColor(android.R.color.black));
                // Tùy chỉnh thêm style cho đẹp nếu cần
                layoutIngredientsList.addView(checkBox);
            }
        }

        // 4. Xử lý danh sách Cách làm (Dynamic Layout Inflater)
        layoutStepsList.removeAllViews();
        List<String> cachLams = monAnHienTai.getCachLam();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (cachLams != null) {
            for (int i = 0; i < cachLams.size(); i++) {
                // Inflate layout con
                View stepView = inflater.inflate(R.layout.item_step_food, layoutStepsList, false);

                TextView tvNum = stepView.findViewById(R.id.tvStepNumber);
                TextView tvContent = stepView.findViewById(R.id.tvStepContent);

                tvNum.setText(String.valueOf(i + 1));
                tvContent.setText(cachLams.get(i));

                layoutStepsList.addView(stepView);
            }
        }

         //5. Kiểm tra trạng thái yêu thích (Nếu có ID người dùng)
         String currentUserId = "lay_tu_firebase_auth";
         if (monAnHienTai.getLikedBy().contains(currentUserId)) {
             btnSaveFavorite.setImageResource(R.drawable.ic_heart_fill);
         }
    }

    private void handleEvents() {
        btnBackDetail.setOnClickListener(v -> finish());

        btnSaveFavorite.setOnClickListener(v -> {
            // Đây là logic xử lý UI tạm thời
            // Về sau bạn sẽ gọi hàm update lên Firestore ở đây
            boolean isSelected = btnSaveFavorite.isSelected(); // Dùng biến cờ hoặc check drawable

            // Ví dụ logic đơn giản để đổi icon:
            // Bạn cần logic check xem hiện tại đang like hay không like
            // Tạm thời mình giả lập toggle:
            if (btnSaveFavorite.getTag() == null || btnSaveFavorite.getTag().equals("unlike")) {
                btnSaveFavorite.setImageResource(R.drawable.ic_heart_fill); // Cần có icon tim đặc
                btnSaveFavorite.setTag("liked");
                Toast.makeText(this, "Đã thích món " + monAnHienTai.getTenMon(), Toast.LENGTH_SHORT).show();

                // TODO: Thêm User ID vào list likedBy và update Firestore tăng likeCount
            } else {
                btnSaveFavorite.setImageResource(R.drawable.ic_heart_outline);
                btnSaveFavorite.setTag("unlike");

                // TODO: Xóa User ID khỏi list likedBy và update Firestore giảm likeCount
            }
        });
    }
}