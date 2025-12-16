package ntu.nguyentruong.smartrecipeapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.UUID;


public class ProfileFragment extends Fragment {

    // Khai báo các biến View
    private ImageView imgAvatar;
    private TextView tvUserName, tvUserEmail, btnCreateNewRecipe;
    private MaterialButton btnLogout;
    private RecyclerView recyclerMyRecipes;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        // 1. Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Ánh xạ View (Khớp ID với XML bạn cung cấp)
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        btnCreateNewRecipe = view.findViewById(R.id.btnCreateNewRecipe); // Nút "+ Tạo mới"
        btnLogout = view.findViewById(R.id.btnLogout); // Nút Đăng xuất
        recyclerMyRecipes = view.findViewById(R.id.recyclerMyRecipes);

        // 3. Setup RecyclerView (Danh sách món ăn ngang)
        setupMyRecipesRecycler();

        // 4. Load dữ liệu người dùng
        loadUserProfile();

        // 5. Bắt sự kiện Click
        setupEvents();

        return view;
    }
    private void setupEvents() {
        // Sự kiện Đăng xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            // Xóa back stack để không quay lại được
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Sự kiện nút "+ Tạo mới"
        btnCreateNewRecipe.setOnClickListener(v -> {
            // Chuyển sang Activity đăng món ăn (Bạn cần tạo Activity này sau)
//            startActivity(new Intent(getActivity(), AddFoodsActivity2.class));
//            Toast.makeText(getContext(), "Chuyển sang trang đề xuât món ăn", Toast.LENGTH_SHORT).show();
            addSampleData();
        });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Load từ Firestore
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // Set Tên và Email
                            tvUserName.setText(user.getFullName());
                            tvUserEmail.setText(user.getEmail());

                            // Set Avatar dùng Glide
                            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                                Glide.with(this)
                                        .load(user.getAvatarUrl())
                                        .placeholder(R.mipmap.ic_launcher) // Ảnh chờ
                                        .error(R.mipmap.ic_launcher)       // Ảnh lỗi
                                        .into(imgAvatar);
                            }

                            // (Nâng cao) Tại đây bạn có thể load thêm số lượng bài post/like
                            // nếu trong model User có lưu hoặc query count từ collection recipes
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi tải thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupMyRecipesRecycler() {
        // Cấu hình hiển thị danh sách nằm ngang (Horizontal)
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerMyRecipes.setLayoutManager(layoutManager);

        // TODO: Sau này bạn cần tạo Adapter và setAdapter ở đây để hiển thị món ăn thật
        // MyRecipeAdapter adapter = new MyRecipeAdapter(danhSachMonAn);
        // recyclerMyRecipes.setAdapter(adapter);
    }
    private void addSampleData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // 1. Lấy dữ liệu mẫu từ class DataSeeder
        List<MonAn> sampleList = DataSeeder.getSampleRecipes(user.getUid(), user.getDisplayName());

        // 2. Duyệt vòng lặp và đẩy từng món lên Firestore
        for (MonAn mon : sampleList) {
            // Tạo ID ngẫu nhiên cho món ăn
            String recipeId = UUID.randomUUID().toString();
            mon.setId(recipeId); // Set ID vào object luôn

            db.collection("recipes").document(recipeId)
                    .set(mon)
                    .addOnSuccessListener(aVoid -> {
                        // Log hoặc Toast nhẹ để biết
                        System.out.println("Đã thêm món: " + mon.getTenMon());
                    })
                    .addOnFailureListener(e -> {
                        System.out.println("Lỗi thêm món: " + e.getMessage());
                    });
        }
    }
}