package com.minminaya.crop;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CropView mCropView;
    private RecyclerView mRecyclerView;
    private CropRecyclerViewAdapter mRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.rv);
        mCropView = findViewById(R.id.crop_img);
        mRecyclerViewAdapter = new CropRecyclerViewAdapter();

        mCropView.setImageResource(R.mipmap.test_pic);

        if (mRecyclerView != null) {
            mRecyclerViewAdapter = new CropRecyclerViewAdapter();
            // 数据来源
            mRecyclerViewAdapter.setCommonAdapterBean(handleRvAdapterData());
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            mRecyclerView.setAdapter(mRecyclerViewAdapter);
            mRecyclerViewAdapter.setOnItemClickedListener(new CropRecyclerViewAdapter.OnItemClickedListener() {
                @Override
                public void onClicked(View view, int position) {
                    handleRvItemClicked(view, position);
                }
            });
        }
    }

    private CommonAdapterBean handleRvAdapterData() {
        CommonAdapterBean CommonAdapterBean = new CommonAdapterBean();
        List<Integer> funcPics = CommonAdapterBean.getFuncPics();
        List<String> funcNames = CommonAdapterBean.getFuncNames();
        String[] funcNameArrays =
                new String[] {Constant.CropBean.STR_ROTATION, Constant.CropBean.STR_REVERSION,
                        Constant.CropBean.STR_RATIO_FREE, Constant.CropBean.STR_RATIO_SQUARE,
                        Constant.CropBean.STR_RATIO_2_3, Constant.CropBean.STR_RATIO_3_2,
                        Constant.CropBean.STR_RATIO_3_4, Constant.CropBean.STR_RATIO_4_3,
                        Constant.CropBean.STR_RATIO_9_16, Constant.CropBean.STR_RATIO_16_9};
        for (String funcName : funcNameArrays) {
            funcPics.add(R.mipmap.ic_launcher);
            funcNames.add(funcName);
        }
        return CommonAdapterBean;
    }

    protected void handleRvItemClicked(View view, int position) {
        switch (position) {
            case Constant.CropBean.INDEX_ROTATION: {
                mCropView.rotateImage(CropView.RotateDegreesEnum.ROTATE_M90D);
                break;
            }
            case Constant.CropBean.INDEX_REVERSION: {
                mCropView.reverseY();
                break;
            }
            case Constant.CropBean.INDEX_RATIO_FREE: {
                mCropView.setCropMode(CropView.CropModeEnum.FREE);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_SQUARE: {
                mCropView.setCropMode(CropView.CropModeEnum.SQUARE);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_2_3: {
                mCropView.setCropMode(CropView.CropModeEnum.RATIO_2_3);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_3_2: {
                mCropView.setCropMode(CropView.CropModeEnum.RATIO_3_2);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_3_4: {
                mCropView.setCropMode(CropView.CropModeEnum.RATIO_3_4);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_4_3: {
                mCropView.setCropMode(CropView.CropModeEnum.RATIO_4_3);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_9_16: {
                mCropView.setCropMode(CropView.CropModeEnum.RATIO_9_16);
                break;
            }
            case Constant.CropBean.INDEX_RATIO_16_9: {
                mCropView.setCropMode(CropView.CropModeEnum.RATIO_16_9);
                break;
            }
        }
    }
}
