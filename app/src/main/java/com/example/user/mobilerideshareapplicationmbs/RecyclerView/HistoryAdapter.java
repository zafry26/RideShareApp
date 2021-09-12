package com.example.user.mobilerideshareapplicationmbs.RecyclerView;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.user.mobilerideshareapplicationmbs.R;
import com.example.user.mobilerideshareapplicationmbs.SingleHistoryFragment;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>  {

    private List<HistoryObject> itemList;
    private Activity context;
    private FragmentManager fragmentManager;


    public HistoryAdapter(Activity context, List<HistoryObject> itemList, FragmentManager fragmentManager1) {
        this.context = context;
        this.itemList = itemList;
        this.fragmentManager = fragmentManager1;
    }

    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);
        HistoryViewHolder rcv = new HistoryViewHolder(layoutView);
        return rcv;
    }

    @Override
    public void onBindViewHolder(HistoryViewHolder holder, final int position) {
        holder.rideId.setText(itemList.get(position).getRideId());
        if(itemList.get(position).getTime()!=null){
            holder.time.setText(itemList.get(position).getTime());
        }
        holder.userType.setText(itemList.get(position).getUserType());
    }
    @Override
    public int getItemCount() {
        return this.itemList.size();
    }

    public class HistoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView rideId;
        public TextView time;
        public TextView userType;

        public HistoryViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            rideId = (TextView) itemView.findViewById(R.id.rideId);

            time = (TextView) itemView.findViewById(R.id.time);

            userType = (TextView) itemView.findViewById(R.id.userType);

        }

        @Override
        public void onClick(View v) {

            String user = userType.getText().toString().trim();

            String rideID = rideId.getText().toString().trim();

            Fragment singleHistoryFragment = new SingleHistoryFragment();
            Bundle bundle = new Bundle();
            bundle.putString("rideId", rideID);
            singleHistoryFragment.setArguments(bundle);

            if (user.equals("Driver")) {

                fragmentManager.beginTransaction().replace(R.id.driver_dashboard_layout, singleHistoryFragment).commit();
            }
            else{
                fragmentManager.beginTransaction().replace(R.id.rider_dashboard_layout, singleHistoryFragment).commit();
            }
        }
    }

}
