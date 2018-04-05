package com.danielebufarini.reminders2.ui;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {
    private int numOfTabs;

    public PagerAdapter(FragmentManager fm, int numOfTabs) {

        super(fm);
        this.numOfTabs = numOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                TimeBasedReminderFragment tab1 = TimeBasedReminderFragment.newInstance("", "");
                return tab1;
            case 1:
                LocationBasedReminderFragment tab2 = new LocationBasedReminderFragment();
                return tab2;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {

        return numOfTabs;
    }
}
