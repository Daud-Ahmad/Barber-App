package com.qtt.thebarber.Adapter;


import com.qtt.thebarber.Model.LookBook;

import java.util.List;

public class HomeSliderAdapter  {

    List<LookBook> bannerList;

    public HomeSliderAdapter(List<LookBook> bannerList) {
        this.bannerList = bannerList;
    }

//    @Override
//    public int getItemCount() {
//        return bannerList.size();
//    }
//
//    @Override
//    public void onBindImageSlide(int position, ImageSlideViewHolder imageSlideViewHolder) {
//        imageSlideViewHolder.bindImageSlide(bannerList.get(position).getUrl());
//    }
}
