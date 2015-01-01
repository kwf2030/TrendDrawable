package org.knight.widget;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Random;

import static org.knight.widget.R.id.img;

public class MainActivity extends Activity {
    private ImageView mImageView;

    private TrendDrawable mTrendDrawable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mImageView = (ImageView) findViewById(img);
    }

    public void onClick(View v) {
        ArrayList<Float> data = mockData();
        if (mTrendDrawable == null) {
            mTrendDrawable = new TrendDrawable(data);
            mImageView.setImageDrawable(mTrendDrawable);
        } else {
            mTrendDrawable.refresh(data);
        }
        mTrendDrawable.startAnimation();
    }

    private ArrayList<Float> mockData() {
        ArrayList<Float> ret = new ArrayList<Float>();
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            ret.add(r.nextFloat() * 100 - 50);
        }
        return ret;
    }
}
