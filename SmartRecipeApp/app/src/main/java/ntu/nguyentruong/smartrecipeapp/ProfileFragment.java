package ntu.nguyentruong.smartrecipeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    // View
    private ImageView imgAvatar;
    private TextView tvUserName, tvUserEmail, tvUserBio; // Thêm Bio
    private TextView tvPostCount, tvLikeCount, tvPendingCount; // Đổi Plan -> Pending
    private TextView btnCreateNewRecipe, btnEditProfile;
    private MaterialButton btnLogout;
    private RecyclerView recyclerMyRecipes;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private MyRecipeAdapter adapter;
    private List<MonAn> myRecipeList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupMyRecipesRecycler();

        // Load Data
        loadUserProfile();
        loadUserStats(); // Hàm mới để load các con số

        setupEvents();
        return view;
    }

    private void initViews(View view) {
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserBio = view.findViewById(R.id.tvUserBio); // Ánh xạ Bio

        tvPostCount = view.findViewById(R.id.tvPostCount);
        tvLikeCount = view.findViewById(R.id.tvLikeCount);
        tvPendingCount = view.findViewById(R.id.tvPendingCount); // Ánh xạ Pending

        btnCreateNewRecipe = view.findViewById(R.id.btnCreateNewRecipe);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        recyclerMyRecipes = view.findViewById(R.id.recyclerMyRecipes);
    }

    private void setupEvents() {
        // 1. Xử lý Đăng xuất có xác nhận (Alert Dialog)
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        // 2. Chuyển sang trang tạo món
        btnCreateNewRecipe.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AddFoodsActivity2.class));
        });

        // 3. Chuyển sang trang chỉnh sửa Profile
        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditProfileActivity.class));
        });
    }

    // --- LOGIC DIALOG ĐĂNG XUẤT ---
    private void showLogoutDialog() {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // --- LOGIC LOAD SỐ LIỆU ---
    private void loadUserStats() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        // 1. Đếm số bài viết (Tổng các bài của user)
        db.collection("recipes").whereEqualTo("authorId", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        tvPostCount.setText(String.valueOf(snapshots.size()));

                        // Cập nhật list cho RecyclerView luôn ở đây nếu muốn realtime
                        myRecipeList.clear();
                        myRecipeList.addAll(snapshots.toObjects(MonAn.class));
                        adapter.notifyDataSetChanged();
                    }
                });

        // 2. Đếm số bài Đang chờ (Status == "pending")
        db.collection("recipes")
                .whereEqualTo("authorId", uid)
                .whereEqualTo("status", "pending") // Đảm bảo lúc upload bạn set status là "pending"
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        tvPendingCount.setText(String.valueOf(snapshots.size()));
                    }
                });

        // 3. Đếm số Yêu thích
        // Giả sử bạn có collection "favorites" lưu { userId: "...", recipeId: "..." }
        db.collection("favorites")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        tvLikeCount.setText(String.valueOf(snapshots.size()));
                    } else {
                        tvLikeCount.setText("0");
                    }
                });
    }

    private void loadUserProfile() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        // Dùng addSnapshotListener để khi sửa profile xong quay lại nó tự cập nhật
        db.collection("users").document(uid)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            tvUserName.setText(user.getFullName());
                            tvUserEmail.setText(user.getEmail());

                            // Hiển thị Bio
                            if (user.getBio() != null && !user.getBio().isEmpty()) {
                                tvUserBio.setText(user.getBio());
                            } else {
                                tvUserBio.setText("Người dùng này chưa viết gì về bản thân.");
                            }

                            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                                Glide.with(this).load(user.getAvatarUrl()).circleCrop().into(imgAvatar);
                            }
                        }
                    }
                });
    }

    private void setupMyRecipesRecycler() {
        myRecipeList = new ArrayList<>();
        adapter = new MyRecipeAdapter(getContext(), myRecipeList);
        recyclerMyRecipes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerMyRecipes.setAdapter(adapter);
    }
}