package ntu.nguyentruong.smartrecipeapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MyRecipeAdapter extends RecyclerView.Adapter<MyRecipeAdapter.MyRecipeViewHolder> {

    private Context context;
    private List<MonAn> monAnList;

    public MyRecipeAdapter(Context context, List<MonAn> monAnList) {
        this.context = context;
        this.monAnList = monAnList;
    }

    @NonNull
    @Override
    public MyRecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mini, parent, false);
        return new MyRecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyRecipeViewHolder holder, int position) {
        MonAn monAn = monAnList.get(position);

        holder.tvFoodName.setText(monAn.getTenMon());

        // Load ảnh bằng Glide
        Glide.with(context)
                .load(monAn.getHinhAnh())
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(holder.imgFood);
        Intent intent = new Intent(context, AddFoodsActivity2.class);
        // Gửi Object hoặc ID sang Activity kia
        intent.putExtra("RECIPE_ID", monAn.getId());
        // Flag để biết là đang sửa chứ không phải tạo mới
        intent.putExtra("IS_EDIT", true);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return monAnList.size();
    }

    public static class MyRecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFood;
        TextView tvFoodName;

        public MyRecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFood = itemView.findViewById(R.id.imgFood);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
        }
    }
}