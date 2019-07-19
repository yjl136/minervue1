package com.ramy.minervue.camera;

import android.graphics.Color;
import android.hardware.Camera;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.ramy.minervue.R;
import com.ramy.minervue.media.VideoCodec;

import java.util.List;
import java.util.jar.Attributes;

/**
 * Created by peter on 11/30/13.
 */
public class PreviewSizeAdapter extends BaseAdapter {

    private List<Camera.Size> sizeList;

    public PreviewSizeAdapter(List<Camera.Size> sizeList) {
        this.sizeList = sizeList;
    }

    @Override
    public int getCount() {
        return sizeList.size();
    }

    @Override
    public Camera.Size getItem(int position) {
        return sizeList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public int getItemPosition(Camera.Size size) {
        return sizeList.indexOf(size);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.preview_size_item, parent, false);
        }
        TextView tv = (TextView) convertView;
        Camera.Size size = (Camera.Size) getItem(position);
        tv.setTextColor(Color.BLACK);
        tv.setText(size.width + "x" + size.height);
        
        return tv;
    }

}
