package com.example.pickleball.adapter;

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.model.User;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private List<User> userList;

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.txtName.setText(user.getFullName());
        holder.txtEmail.setText(user.getEmail());

        // Hiển thị chữ cái đầu của tên làm Avatar
        if (user.getFullName() != null && !user.getFullName().isEmpty()) {
            holder.tvAvatarChar.setText(user.getFullName().substring(0, 1).toUpperCase());
        }

        // Thiết lập trạng thái ban đầu cho Switch và Label
        // Lưu ý: isBlocked = true thì Switch tắt (Unchecked), Label = "Đã khóa"
        updateUIStatus(holder, user.isBlocked());

        // Xóa listener cũ trước khi setChecked để tránh bị trigger oan khi scroll
        holder.switchLock.setOnCheckedChangeListener(null);
        holder.switchLock.setChecked(!user.isBlocked());

        // Sự kiện gạt công tắc
        holder.switchLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            boolean isBlocking = !isChecked; // Nếu tắt switch (false) -> Blocked = true

            // Cập nhật lên Firestore
            FirebaseFirestore.getInstance().collection("Users")
                    .document(user.getUserId())
                    .update("blocked", isBlocking)
                    .addOnSuccessListener(aVoid -> {
                        user.setBlocked(isBlocking);
                        updateUIStatus(holder, isBlocking);
                        String msg = isBlocking ? "Đã khóa tài khoản" : "Đã mở khóa tài khoản";
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Reset lại trạng thái switch nếu lỗi
                        holder.switchLock.setChecked(!user.isBlocked());
                        Toast.makeText(context, "Lỗi cập nhật!", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void updateUIStatus(UserViewHolder holder, boolean isBlocked) {
        if (isBlocked) {
            holder.txtStatus.setText("Đã khóa");
            holder.txtStatus.setTextColor(ContextCompat.getColor(context, R.color.error_red));
        } else {
            holder.txtStatus.setText("Hoạt động");
            holder.txtStatus.setTextColor(ContextCompat.getColor(context, R.color.green_primary));
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtEmail, txtStatus, tvAvatarChar;
        SwitchMaterial switchLock;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.tvUserName);
            txtEmail = itemView.findViewById(R.id.tvUserEmail);
            txtStatus = itemView.findViewById(R.id.tvStatusLabel);
            tvAvatarChar = itemView.findViewById(R.id.tvAvatarChar);
            switchLock = itemView.findViewById(R.id.switchLock);
        }
    }
}
