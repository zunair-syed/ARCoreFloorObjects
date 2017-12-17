package com.google.ar.core.examples.java.helloar.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.ar.core.examples.java.helloar.HelloArActivity;
import com.google.ar.core.examples.java.helloar.R;
import com.google.ar.core.examples.java.helloar.model.ObjectsModel;

import java.util.List;

/**
 * Created by zunairsyed on 2017-12-13.
 */

public class ModelSelectorAdapter extends RecyclerView.Adapter<ModelSelectorAdapter.ViewHolder> {

    private ObjectsModel[] data;
    private Context context;
    private int lastChosenPosition = 0;

    public ModelSelectorAdapter(ObjectsModel[] data, Context context) {
        this.data = data;
        this.context = context;
        lastChosenPosition = data.length - 1;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.model_item_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((HelloArActivity)context).onClickModel(data[position], position);
                lastChosenPosition = position;
                ModelSelectorAdapter.this.notifyDataSetChanged();
            }
        });
        holder.modelNameText.setText(data[position].getName());
        Glide.with(holder.itemView.getContext())
                .load(data[position].getPreview())
                .into(holder.modelPreviewImage);

        if(position == lastChosenPosition) holder.itemView.setBackgroundColor(Color.parseColor("#B3E5FC"));
        else holder.itemView.setBackgroundColor(Color.WHITE);
    }

    @Override
    public int getItemCount() {
        return data.length;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView modelPreviewImage;
        private TextView modelNameText;

        public ViewHolder(View itemView) {
            super(itemView);
            modelPreviewImage = (ImageView) itemView.findViewById(R.id.modelPreviewImage);
            modelNameText = (TextView) itemView.findViewById(R.id.modelNameText);

        }
    }
}