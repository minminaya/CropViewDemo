package com.minminaya.crop;

import java.util.ArrayList;
import java.util.List;

public class CommonAdapterBean {
    private List<String> mFuncNames;
    private List<Integer> mFuncPics;

    public CommonAdapterBean() {
        mFuncNames = new ArrayList<>();
        mFuncPics = new ArrayList<>();
    }

    public List<String> getFuncNames() {
        return mFuncNames;
    }

    public void setFuncNames(List<String> funcNames) {
        mFuncNames = funcNames;
    }

    public List<Integer> getFuncPics() {
        return mFuncPics;
    }

    public void setFuncPics(List<Integer> funcPics) {
        mFuncPics = funcPics;
    }
}
