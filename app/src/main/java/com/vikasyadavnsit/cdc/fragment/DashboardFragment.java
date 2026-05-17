package com.vikasyadavnsit.cdc.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.vikasyadavnsit.cdc.R;
import com.vikasyadavnsit.cdc.utils.CommonUtil;

public class DashboardFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        view.findViewById(R.id.dashboard_todo_card).setOnClickListener(v ->
                CommonUtil.loadFragmentWithBackStack(getParentFragmentManager(), new TodoListFragment()));
        view.findViewById(R.id.dashboard_spending_card).setOnClickListener(v ->
                CommonUtil.loadFragmentWithBackStack(getParentFragmentManager(), new SpendingFragment()));
        return view;
    }
}
