package com.tehnicomsolutions.androidsocket.server;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tehnicomsolutions.androidsocket.socketlibrary.Constants;
import com.tehnicomsolutions.androidsocket.socketlibrary.Data;
import com.tehnicomsolutions.androidsocket.socketlibrary.Utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class MainActivity extends Activity implements Runnable
{
    public static final int SERVER_PORT = 6000;
    Thread serverThread = null;
    private ServerSocket serverSocket;
    private Handler uiHandler;
    TextView tvMessages;
    Button dragView;
    ImageView ivImage;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initDragView();

        tvMessages = (TextView)findViewById(R.id.tvMessages);
        ivImage = (ImageView)findViewById(R.id.ivImage);

        uiHandler = new Handler();
        serverThread = new Thread(this);
        serverThread.start();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        try
        {
            serverSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void initDragView()
    {
        WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowParams.x = 200;
        mWindowParams.y = 200;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                //| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                //| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                /*| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS*/;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;

        dragView = new Button(this);
        dragView.setText("Drag Button");

        WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(dragView, mWindowParams);
    }

    // move the drag view
    private void move(int x, int y)
    {
        if (dragView != null)
        {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) dragView.getLayoutParams();
            layoutParams.x = x;
            layoutParams.y = y;
            WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            mWindowManager.updateViewLayout(dragView, layoutParams);
        }
    }


    @Override
    public void run()
    {
        Socket socket;

        try
        {
            serverSocket = new ServerSocket(SERVER_PORT);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        while (!Thread.currentThread().isInterrupted())
        {
            try
            {
                if(serverSocket == null || serverSocket.isClosed())return;
                socket = serverSocket.accept();
                CommunicationThread commThread = new CommunicationThread(socket);
                sendServerInfoToClient(socket);
                new Thread(commThread).start();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }

    }

    private void sendServerInfoToClient(Socket socket) throws IOException
    {
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        out.println("<data type=server_config>\nwidth=" + point.x + "\nheight=" + point.y + "\norientation=" + getResources().getConfiguration().orientation + "\n</data>");
    }

    class CommunicationThread implements Runnable
    {
        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket)
        {
            this.clientSocket = clientSocket;
            try
            {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        public void run()
        {
            Data data = new Data();
            while (!Thread.currentThread().isInterrupted())
            {
                try
                {
                    final String read = input.readLine();
                    if(read == null)return;
                    if(read.startsWith(Constants.DATA_START_TAG))
                    {
                        data.start();
                        String type = read.split(" ")[1].split("=")[1];
                        data.type = Data.Type.valueOf(type.substring(0, type.length() - 1));
                        continue;
                    }
                    if(Constants.DATA_END_TAG.equals(read))
                    {
                        if(data.isStarted())
                        {
                            String message = data.getData();
                            Data.Type type = data.type;
                            onMessageReceived(message, type);
                            data.close();
                        }
                        else
                        {
                            Log.w(Constants.LOG_TAG, "received closing tag, but there was no starting tag");
                        }
                        continue;
                    }
                    if(data.isStarted())
                    {
                        data.append(read).append("\n");
                    }
                    else
                    {
                        Log.w(Constants.LOG_TAG, "received some data, but there was no starting tag, data will be ignored");
                    }

                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

    }

    public void onMessageReceived(final String message, Data.Type type)
    {
        if (type == Data.Type.coordinates)
        {
            int x = 0, y = 0;
            String[] data = message.split("\n");
            for(String str : data)
            {
                if(str != null && str.startsWith("x"))
                {
                    x = Utility.parseInt(str.split("=")[1], 0);
                }
                else if(str != null && str.startsWith("y"))
                {
                    y = Utility.parseInt(str.split("=")[1], 0);
                }
            }
            final int finalY = y;
            final int finalX = x;
            uiHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    move(finalX, finalY);
                    tvMessages.setText(/*tvMessages.getText().toString()+ */message/* + "\n"*/);
                }
            });
        }
        else if(type == Data.Type.plain_text)
        {
            uiHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    tvMessages.setText(/*tvMessages.getText().toString()+ */message/* + "\n"*/);
                }
            });
        }
        else if(type == Data.Type.bitmap)
        {
            uiHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    ivImage.setImageBitmap(Utility.decodeBase64(message));
                }
            });

        }

    }
}
