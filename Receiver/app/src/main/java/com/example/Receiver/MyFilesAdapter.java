package com.example.Receiver;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyFilesAdapter extends RecyclerView.Adapter<MyFilesAdapter.VH> implements View.OnClickListener {
    Context context;
    List<String> data;
    private OnItemClickListener mOnItemClickListener;


    static class VH extends RecyclerView.ViewHolder{
        LinearLayout item_container;
        TextView filename;

        public VH(@NonNull View itemView) {
            super(itemView);
            item_container = itemView.findViewById(R.id.item_container);
            filename = itemView.findViewById(R.id.filename);
        }
    }

    public interface OnItemClickListener{
        void OnItemClick(String name);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        this.mOnItemClickListener = onItemClickListener;
    }

    @Override
    public void onClick(View v) {
        if(mOnItemClickListener != null){
            int position = (int)v.getTag();
            mOnItemClickListener.OnItemClick(data.get(position));
        }else {
            throw new NullPointerException();
        }
    }

    public MyFilesAdapter(Context context, List<String> data) {
        this.context = context;
        this.data = data;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recyclerview_item, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.filename.setText(data.get(position));
        holder.item_container.setTag(position);
        holder.item_container.setOnClickListener(this);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void updateData(List<String> data){
        this.data = data;
        notifyDataSetChanged();
    }
}
