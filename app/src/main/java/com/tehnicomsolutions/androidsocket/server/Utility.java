package com.tehnicomsolutions.androidsocket.server;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

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
            return ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        }
        else if(confOrientation == Configuration.ORIENTATION_PORTRAIT)
        {
            return ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        }
        return confOrientation;
    }

    public static String encodeToBase64(Bitmap image)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    public static Bitmap decodeBase64(String input)
    {
        byte[] decodedByte = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }
}
