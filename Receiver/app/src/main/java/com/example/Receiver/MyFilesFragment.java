package com.example.Receiver;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class MyFilesFragment extends Fragment implements MyFilesAdapter.OnItemClickListener{
    //    app 下载根目录
    //    同 MainActivity
    String app_download_directory;

//    适配器
    MyFilesAdapter adapter;

//    适配器使用的文件名数据
    List<String> filenames_list;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_files, container, false);

        Bundle activity_data = Objects.requireNonNull(getArguments());
        app_download_directory = Objects.requireNonNull(activity_data.getString("app_download_directory"));

//        刷新数据
        flushFilenamesList();

        adapter = new MyFilesAdapter(getContext(), filenames_list);
        adapter.setOnItemClickListener(this);
        RecyclerView recycler_view = view.findViewById(R.id.recyclerview);
        recycler_view.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler_view.setAdapter(adapter);
        recycler_view.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));


        return view;
    }

//    刷新 filenames_list
//    如果文件夹是空的那么 filenames_list 也是空的
    private void flushFilenamesList(){
        if(filenames_list == null){
            filenames_list = new LinkedList<>();
        }
        filenames_list.clear();

        File[] files = new File(app_download_directory).listFiles();
        for(File file : files){
            if(file.isFile()){
                filenames_list.add(file.getName());
            }
        }
    }

//    每次 show fragment 的时候都刷新数据
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            flushFilenamesList();
            adapter.updateData(filenames_list);
        }
    }

    @Override
    public void OnItemClick(String name) {
        String path = app_download_directory + File.separator + name;

        Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileProvider", new File(path));
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
