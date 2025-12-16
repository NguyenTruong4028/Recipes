package ntu.nguyentruong.smartrecipeapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MyRecipeAdapter extends RecyclerView.Adapter<MyRecipeAdapter.MyRecipeViewHolder>{
    private Context context;
    private List<MonAn> listMonAn;

    public MyRecipeAdapter(Context context, List<MonAn> listMonAn) {
        this.context = context;
        this.listMonAn = listMonAn;
    }

    @NonNull
    @Override
    public MyRecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mini,parent,false);
        return new MyRecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyRecipeViewHolder holder, int position) {
        MonAn mon = listMonAn.get(position);
        holder.tvName.setText(mon.getTenMon());;

        // Load ảnh món ăn
        Glide.with(context).load(mon.getHinhAnh()).into(holder.imgMon);
    }

    @Override
    public int getItemCount() {
        return listMonAn.size();
    }


    public static class MyRecipeViewHolder extends RecyclerView.ViewHolder {

        ImageView imgMon;
        TextView tvName;

        public MyRecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMon = itemView.findViewById(R.id.imgFoodMini);
            tvName = itemView.findViewById(R.id.tvNameMini);

        }
    }
}
