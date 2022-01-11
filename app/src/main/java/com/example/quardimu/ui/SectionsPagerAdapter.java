package com.example.quardimu.ui;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.quardimu.R;
import com.example.quardimu.ui.GPS.GPSFragment;
import com.example.quardimu.ui.Plot.PlotFragment;

public class SectionsPagerAdapter extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.title_monitor,R.string.pref_title_GPS, R.string.pref_title_plot, R.string.title_records};
    private final Context mContext;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        switch (position){
            case 1:
                return GPSFragment.newInstance();
            case 2:
                return PlotFragment.newInstance();
            case 3:
                return RecordsFragment.newInstance();
            default:
                return MonitorFragment.newInstance();
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        // Show 2 total pages.
        return 4;
    }
}