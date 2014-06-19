package com.tehnicomsolutions.androidsocket.socketlibrary;

/**
 * Created by pedja on 19.6.14. 15.08.
 * This class is part of the AndroidSocket
 * Copyright © 2014 ${OWNER}
 */
public class Data
{
    private StringBuilder builder = null;
    public Type type;

    public enum Type
    {
        server_config, bitmap, plain_text, coordinates
    }

    public void start()
    {
        builder = new StringBuilder();
    }

    public boolean isStarted()
    {
        return builder != null;
    }

    public Data append(String string)
    {
        builder.append(string);
        return this;
    }

    public String close()
    {
        String data = builder.toString();
        builder = null;
        return data;
    }


}
