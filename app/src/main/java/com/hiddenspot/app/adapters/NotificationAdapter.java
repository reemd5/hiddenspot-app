package com.hiddenspot.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hiddenspot.app.R;
import com.hiddenspot.app.models.AppNotification;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification, int position);
    }

    private final Context context;
    private List<AppNotification> notifications;
    private OnNotificationClickListener onNotificationClickListener;

    public NotificationAdapter(Context context, List<AppNotification> notifications) {
        this.context = context;
        this.notifications = notifications;
    }

    public void updateNotifications(List<AppNotification> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    public void setOnNotificationClickListener(OnNotificationClickListener onNotificationClickListener) {
        this.onNotificationClickListener = onNotificationClickListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        AppNotification notification = notifications.get(position);
        holder.tvMessage.setText(notification.getMessage());
        holder.tvTime.setText(notification.getFormattedTimestamp());
        holder.itemView.setOnClickListener(v -> {
            if (onNotificationClickListener != null) {
                onNotificationClickListener.onNotificationClick(notification, holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMessage;
        final TextView tvTime;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_notification_message);
            tvTime = itemView.findViewById(R.id.tv_notification_time);
        }
    }
}
