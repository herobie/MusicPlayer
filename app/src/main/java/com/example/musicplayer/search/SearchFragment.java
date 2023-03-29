package com.example.musicplayer.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.Constant;
import com.example.musicplayer.R;
import com.example.musicplayer.Sqlite.CRUD;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.material.textfield.TextInputEditText;

public class SearchFragment extends Fragment implements View.OnClickListener{
    private RecyclerView search_rv;
    private TextInputEditText search_input;
    private SearchAdapter searchAdapter;
    private ImageButton search_delete;
    private CRUD crud;
    public View onCreateView(LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_search, container , false);
        return view;
    }

    public void onViewCreated(View view , Bundle savedInstanceState){
        super.onViewCreated(view , savedInstanceState);
        initView(view);
    }

    public void initView(View view){
        search_input = view.findViewById(R.id.search_input);
        search_delete = view.findViewById(R.id.search_delete);
        search_delete.setOnClickListener(this);
        search_rv = getView().findViewById(R.id.search_rv);
        search_rv.setOnClickListener(this);
        crud = new CRUD("searchHistory" , getContext());
        crud.retrieveSearchHistory();
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(getContext());
        search_rv.setLayoutManager(layoutManager);
        searchAdapter = new SearchAdapter(Constant.searchHistory, getContext());
        search_rv.setAdapter(searchAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.search_delete:
                crud.clearSearchHistory();
                searchAdapter.notifyDataSetChanged();
                break;

        }
    }
}
