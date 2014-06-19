package com.tehnicomsolutions.androidsocket.socketlibrary;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

/**
 * Created by pedja on 19.6.14. 11.37.
 * This class is part of the AndroidSocket
 * Copyright © 2014 ${OWNER}
 */
public class Utility
{
    public static int parseInt(String mInteger, int defVal)
    {
        try
        {
            return Integer.parseInt(mInteger);
        }
        catch (Exception e)
        {
            return defVal;
        }
    }

    public static int convertConfigurationOrientationToActivityInfoOrientation(int confOrientation)
    {
        if(confOrientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        else if(confOrientation == Configuration.ORIENTATION_PORTRAIT)
        {
            return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
        return confOrientation;
    }
}
