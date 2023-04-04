package com.txtnet.txtnetbrowser.phonenumbers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.txtnet.txtnetbrowser.R;

public class ServerViewHolder extends RecyclerView.ViewHolder{
    public final TextView serverItemView;
    public int id = -1;
    public boolean isDefault = false;
    private ServerViewHolder(View itemView) {
        super(itemView);
        serverItemView = itemView.findViewById(R.id.textView);
    }

    public void bind(String text, boolean isDefault) {

        serverItemView.setText(text);
     //   if(isDefault){
     //       selectedBg();
     //   }
    }

    static ServerViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_item, parent, false);
        return new ServerViewHolder(view);
    }

    public void defaultBg() {
        isDefault = false;
        serverItemView.setBackground(AppCompatResources.getDrawable(serverItemView.getContext(), R.drawable.bg_capsule_unselected));
        serverItemView.setTextColor(ContextCompat.getColor(serverItemView.getContext(), R.color.black));
    }

    public void selectedBg() {
        isDefault = true;
        serverItemView.setBackground(AppCompatResources.getDrawable(serverItemView.getContext(), R.drawable.bg_capsule_selected));
        serverItemView.setTextColor(ContextCompat.getColor(serverItemView.getContext(), R.color.white));

    }

}
