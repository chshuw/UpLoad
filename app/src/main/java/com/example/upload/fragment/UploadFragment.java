package com.example.upload.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.upload.R;
import com.example.upload.utils.AesUtil;
import com.example.upload.utils.GlideImageLoader;
import com.example.upload.utils.ProgressPieView;
import com.example.upload.utils.Urls;
import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.ui.ImageGridActivity;
import com.lzy.okhttpserver.download.DownloadManager;
import com.lzy.okhttpserver.listener.UploadListener;
import com.lzy.okhttpserver.upload.UploadInfo;
import com.lzy.okhttpserver.upload.UploadManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;

public class UploadFragment extends Fragment implements View.OnClickListener {

    public static final String SERVER = "http://10.102.0.158:8080/FileUpload/";
    private GridView gridView;
    private ImagePicker imagePicker;
    private ArrayList<ImageItem> images;
    public static final String key = "00b09e37363e596e1f25b23c78e49939238b";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload, container, false);
        view.findViewById(R.id.select).setOnClickListener(this);
        view.findViewById(R.id.upload).setOnClickListener(this);
        gridView = (GridView) view.findViewById(R.id.gridView);
        return view;
    }

    @Override
    public void onClick(View view) {
//        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            //权限还没有授予，需要在这里写申请权限的代码
//            ActivityCompat.requestPermissions(getActivity(),
//                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
//        } else {
//            //权限已经被授予，在这里直接写要执行的相应方法即可
        switch (view.getId()) {
            case R.id.select:
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    //权限还没有授予，需要在这里写申请权限的代码
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    //权限已经被授予，在这里直接写要执行的相应方法即可
                    imagePicker = ImagePicker.getInstance();
                    imagePicker.setImageLoader(new GlideImageLoader());
                    imagePicker.setShowCamera(true);
                    imagePicker.setSelectLimit(9);
                    imagePicker.setCrop(false);
                    Intent intent = new Intent(getContext(), ImageGridActivity.class);
                    startActivityForResult(intent, 100);

                    break;
                }
            case R.id.upload:
                if (images != null) {
                    for (int i = 0; i < images.size(); i++) {
                        MyUploadListener listener = new MyUploadListener();
                        listener.setUserTag(gridView.getChildAt(i));
//                            UploadManager.getInstance(getContext()).addTask(Urls.SERVER, new File(images.get(i).path), "imageFile", listener);
                        try {
                            UploadManager.getInstance(getContext()).addTask(Urls.SERVER, AesUtil.encryptFile(new File(images.get(i).path),
                                    new File(images.get(i).path), key), "imageFile", listener);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("upload", e.getMessage());
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "不能为空", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
//}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == ImagePicker.RESULT_CODE_ITEMS) {
            if (data != null && requestCode == 100) {
                images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                MyAdapter adapter = new MyAdapter(images);
                gridView.setAdapter(adapter);
            }
        }
    }

private class MyAdapter extends BaseAdapter {

    private List<ImageItem> items;

    public MyAdapter(List<ImageItem> items) {
        this.items = items;
    }

    public void setData(List<ImageItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public ImageItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int size = gridView.getWidth() / 3;
        ViewHolder holder;
        if (convertView == null) {
            convertView = View.inflate(getContext(), R.layout.item_upload_manager, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        imagePicker.getImageLoader().displayImage(getActivity(), getItem(position).path, holder.imageView, size, size);
        return convertView;
    }
}

private class ViewHolder {

    private ImageView imageView;
    private TextView tvProgress;
    private ProgressPieView civ;
    private View mask;

    public ViewHolder(View convertView) {
        imageView = (ImageView) convertView.findViewById(R.id.imageView);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, gridView.getWidth() / 3);
        imageView.setLayoutParams(params);
        tvProgress = (TextView) convertView.findViewById(R.id.tvProgress);
        mask = convertView.findViewById(R.id.mask);
        civ = (ProgressPieView) convertView.findViewById(R.id.civ);
        tvProgress.setText("请上传");
        civ.setText("请上传");
    }

    public void refresh(UploadInfo uploadInfo) {
        if (uploadInfo.getState() == DownloadManager.NONE) {
            tvProgress.setText("请上传");
            civ.setText("请上传");
        } else if (uploadInfo.getState() == UploadManager.ERROR) {
            tvProgress.setText("上传出错");
            civ.setText("错误");
        } else if (uploadInfo.getState() == UploadManager.WAITING) {
            tvProgress.setText("等待中");
            civ.setText("等待");
        } else if (uploadInfo.getState() == UploadManager.FINISH) {
            tvProgress.setText("上传成功");
            civ.setText("成功");
        } else if (uploadInfo.getState() == UploadManager.UPLOADING) {
            tvProgress.setText("上传中");
            civ.setProgress((int) (uploadInfo.getProgress() * 100));
            civ.setText((Math.round(uploadInfo.getProgress() * 10000) * 1.0f / 100) + "%");
        }
    }

    public void finish() {
        tvProgress.setText("上传成功");
        civ.setVisibility(View.INVISIBLE);
        mask.setVisibility(View.INVISIBLE);
    }
}


private class MyUploadListener extends UploadListener<String> {

    private ViewHolder holder;

    @Override
    public void onProgress(UploadInfo uploadInfo) {
        Log.e("MyUploadListener", "onProgress:" + uploadInfo.getFileName() + " " + uploadInfo.getTotalLength() + " " + uploadInfo.getUploadLength() + " " + uploadInfo.getProgress());
        holder = (ViewHolder) ((View) getUserTag()).getTag();
        holder.refresh(uploadInfo);
    }

    @Override
    public void onFinish(String s) {
        Log.e("MyUploadListener", "finish:" + s);
        holder.finish();
    }

    @Override
    public void onError(UploadInfo uploadInfo, String errorMsg, Exception e) {
        Log.e("MyUploadListener", "onError:" + errorMsg);
        Toast.makeText(getContext(), "上传失败", Toast.LENGTH_SHORT).show();
    }

    @Override
    public String parseNetworkResponse(Response response) throws Exception {
        Log.e("MyUploadListener", "parseNetworkResponse");
        return response.body().string();
    }
}

}
