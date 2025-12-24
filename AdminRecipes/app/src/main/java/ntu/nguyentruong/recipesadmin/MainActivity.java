package ntu.nguyentruong.recipesadmin;

import android.os.Bundle;
import android.widget.ImageView;

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

    private void loadPendingRecipes() {
        // Lắng nghe Realtime các bài có status = "pending"
        db.collection("recipes")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    pendingList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            MonAn mon = doc.toObject(MonAn.class);
                            if (mon != null) {
                                mon.setId(doc.getId()); // Lưu ID quan trọng
                                pendingList.add(mon);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}