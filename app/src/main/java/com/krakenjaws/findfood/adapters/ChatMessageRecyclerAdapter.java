package com.krakenjaws.findfood.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.krakenjaws.findfood.R;
import com.krakenjaws.findfood.models.ChatMessage;
import com.krakenjaws.findfood.models.User;

import java.util.ArrayList;

public class ChatMessageRecyclerAdapter extends RecyclerView.Adapter<ChatMessageRecyclerAdapter.ViewHolder> {

    private final ArrayList<ChatMessage> mMessages;
    private final ArrayList<User> mUsers;
    private final Context mContext;

    public ChatMessageRecyclerAdapter(ArrayList<ChatMessage> messages,
                                      ArrayList<User> users,
                                      Context context) {
        this.mMessages = messages;
        this.mUsers = users;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_chat_message_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {


        if (FirebaseAuth.getInstance().getUid().equals(mMessages.get(position).getUser().getUser_id())) {
            holder.username.setTextColor(ContextCompat.getColor(mContext, R.color.green1));
        } else {
            holder.username.setTextColor(ContextCompat.getColor(mContext, R.color.blue2));
        }

        holder.username.setText(mMessages.get(position).getUser().getUsername());
        holder.message.setText(mMessages.get(position).getMessage());
    }


    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView message;
        final TextView username;

        ViewHolder(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.chat_message_message);
            username = itemView.findViewById(R.id.chat_message_username);
        }
    }
}
