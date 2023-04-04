package com.txtnet.txtnetbrowser.phonenumbers;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.txtnet.txtnetbrowser.R;
import com.txtnet.txtnetbrowser.database.Server;

public class ServerListAdapter extends ListAdapter<Server, ServerViewHolder> {
    //SharedPreferences pref;

    int selectedItemPos = -1;
    int lastItemSelectedPos = -1;

    String defaultPhone;
    public ServerListAdapter(@NonNull DiffUtil.ItemCallback<Server> diffCallback, String defaultPhone){
        super(diffCallback);
        this.defaultPhone = defaultPhone;

    }
    @Override
    public ServerViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        return ServerViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(ServerViewHolder holder, int position){
        Server current = getItem(position);
        Log.i("Phone", "current.phoneNumber="+current.phoneNumber + ", defaultPhone:"+defaultPhone + " equals?" + (current.phoneNumber.equals(defaultPhone) ? "true" : "false"));
        holder.bind(current.phoneNumber, current.phoneNumber.equals(defaultPhone));

        holder.id = current.uid;
        if(position == selectedItemPos){
            holder.selectedBg();
        }else{
            holder.defaultBg();
        }
        if((current.phoneNumber.equals(defaultPhone)) && lastItemSelectedPos == -1){
            holder.selectedBg();
            lastItemSelectedPos = holder.getAdapterPosition();
        }else if(position != selectedItemPos && (current.phoneNumber.equals(defaultPhone))){
            holder.defaultBg();
        }
        holder.serverItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedItemPos = holder.getAdapterPosition();
                if(lastItemSelectedPos == -1){
                    lastItemSelectedPos = selectedItemPos;
                }else{
                    notifyItemChanged(lastItemSelectedPos);
                    lastItemSelectedPos = selectedItemPos;
                }
                notifyItemChanged(selectedItemPos);

                SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences(holder.itemView.getContext().getPackageName() + "_preferences", MODE_PRIVATE);
                SharedPreferences.Editor edit = prefs.edit();
                edit.putString(holder.itemView.getContext().getString(R.string.phone_number), (String) holder.serverItemView.getText());
                edit.apply();
                //defaultPhone = (String) holder.serverItemView.getText();
            }
        });



    }
    static class ServerDiff extends DiffUtil.ItemCallback<Server>{
        @Override
        public boolean areItemsTheSame(@NonNull Server oldItem, @NonNull Server newItem){
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Server oldItem, @NonNull Server newItem){
            return oldItem.phoneNumber.equals(newItem.phoneNumber);
        }
    }
}
