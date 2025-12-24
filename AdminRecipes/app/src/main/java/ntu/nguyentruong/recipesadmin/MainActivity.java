package ntu.nguyentruong.recipesadmin;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecipesAdapter adapter;
    private List<MonAn> pendingList;
    private FirebaseFirestore db;
    private ImageView btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);

        db = FirebaseFirestore.getInstance();

        initViews();
        loadPendingRecipes();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerPending);
        btnRefresh = findViewById(R.id.btnRefresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        pendingList = new ArrayList<>();
        adapter = new RecipesAdapter(this, pendingList);
        recyclerView.setAdapter(adapter);

        btnRefresh.setOnClickListener(v -> loadPendingRecipes());
    }

    // Trong MainActivity.java (App Admin)

    private void loadPendingRecipes() {
        // Admin chỉ lấy những bài có status là "pending" trong cùng collection "recipes"
        db.collection("recipes")
                .whereEqualTo("status", "pending") // Phải khớp chữ "pending" bên User gửi lên
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("AdminError", "Lỗi lấy dữ liệu: " + error.getMessage());
                        return;
                    }

                    pendingList.clear();
                    if (value != null && !value.isEmpty()) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            MonAn mon = doc.toObject(MonAn.class);
                            if (mon != null) {
                                mon.setId(doc.getId());
                                pendingList.add(mon);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        // Nếu không có bài nào
                        Toast.makeText(this, "Không có bài chờ duyệt", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}