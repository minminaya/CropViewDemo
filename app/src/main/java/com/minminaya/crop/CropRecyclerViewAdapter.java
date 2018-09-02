package com.minminaya.crop;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * RecyclerView的抽象适配器
 */
public class CropRecyclerViewAdapter extends RecyclerView.Adapter<CropRecyclerViewAdapter.ViewHolder> {
    private CommonAdapterBean mCommonAdapterBean;
    private OnItemClickedListener mOnItemClickedListener;

    public void setCommonAdapterBean(CommonAdapterBean CommonAdapterBean) {
        mCommonAdapterBean = CommonAdapterBean;
    }

    @NonNull
    @Override
    public CropRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        handleViewMargin(holder, position);
        handleViewClicked(holder, position);
        if (mCommonAdapterBean != null) {
            holder.beautyRvItemTv.setText(mCommonAdapterBean.getFuncNames().get(position));
        }
    }

    @Override
    public int getItemCount() {
        return mCommonAdapterBean == null ? 0 : mCommonAdapterBean.getFuncNames().size();
    }

    /**
     * 给起始Item设置左边距
     *
     * @param holder
     * @param position
     */
    protected void handleViewMargin(ViewHolder holder, int position) {

        if (position == 0) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(holder.containerRl.getLayoutParams());
            lp.setMarginStart(SizeUtils.dp2px(30f));
            holder.containerRl.setLayoutParams(lp);
        } else {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(holder.containerRl.getLayoutParams());
            lp.setMarginStart(SizeUtils.dp2px(50f));
            holder.containerRl.setLayoutParams(lp);
        }
    }

    /**
     * 处理item的点击事件
     *
     * @param holder
     * @param position
     */
    private void handleViewClicked(ViewHolder holder, final int position) {
        holder.containerRl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickedListener != null) {
                    mOnItemClickedListener.onClicked(v, position);
                }
            }
        });
    }

    public void setOnItemClickedListener(OnItemClickedListener onItemClickedListener) {
        mOnItemClickedListener = onItemClickedListener;
    }

    public interface OnItemClickedListener {

        void onClicked(View view, int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView beautyRvItemTv;
        RelativeLayout containerRl;

        ViewHolder(View view) {
            super(view);
            beautyRvItemTv = view.findViewById(R.id.tv);
            containerRl = view.findViewById(R.id.container);
        }
    }
}