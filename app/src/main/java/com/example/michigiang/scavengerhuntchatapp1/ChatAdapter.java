package com.example.michigiang.scavengerhuntchatapp1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Michelle Giang giang2
 */

public class ChatAdapter extends ArrayAdapter<ChatRoom> {
    public ChatAdapter(Context context, int resource, List<ChatRoom> objects) {
        super(context, resource, objects);
    }

    //Checks and see if the message is a photo,
    //if it is, then show the photo image view and not the message text view
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.chat_message, parent, false);
        }

        TextView chatRoomTextView = (TextView) convertView.findViewById(R.id.chatTextView);
        ChatRoom room = getItem(position);
        chatRoomTextView.setText(room.getName());

        return convertView;
    }
}
