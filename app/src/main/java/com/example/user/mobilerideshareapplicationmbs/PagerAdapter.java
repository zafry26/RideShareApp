package com.example.user.mobilerideshareapplicationmbs;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {

    public PagerAdapter(FragmentManager fm)
    {
        super(fm);
    }


    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                Login_Page Tab1 = new Login_Page();
                return Tab1;

            case 1:
                Signup_Page Tab2 = new Signup_Page();
                return Tab2;

            default:
                return null;
        }


    }

    @Override
    public int getCount()
    {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position)
        {
            case 0:
                return "Login ";
            case 1:
                return "Signup";
            default:
                return null;
        }
    }
}
