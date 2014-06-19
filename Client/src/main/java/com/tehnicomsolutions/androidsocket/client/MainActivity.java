package com.tehnicomsolutions.androidsocket.client;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
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
import java.net.InetAddress;
import java.net.Socket;


public class MainActivity extends Activity implements Runnable, View.OnClickListener, View.OnTouchListener
{
    private static final int SERVERPORT = 6000;
    private static final String SERVER_IP = "192.168.32.39";

    private Socket socket;
    EditText etMessage;
    Button btnSend;
    TextView tvConnecting;
    Handler uiHandler;
    ServerConfig config;
    Button btnDrag;

    boolean mDragMode;

    int rawXStart;
    int rawYStart;

    int mDragPointOffsetX, mDragPointOffsetY;
    PrintWriter socketOutWriter;
    float screenWidth, screenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        screenWidth = point.x;
        screenHeight = point.y;
        etMessage = (EditText)findViewById(R.id.etMessage);
        btnSend = (Button)findViewById(R.id.btnSend);
        btnSend.setEnabled(false);
        btnSend.setOnClickListener(this);
        tvConnecting = (TextView)findViewById(R.id.tvConnecting);
        tvConnecting.setTextColor(Color.GREEN);
        uiHandler = new Handler();
        new Thread(this).start();

        initDragView();
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

        btnDrag = new Button(this);
        btnDrag.setText("Drag Button");
        btnDrag.setOnTouchListener(this);

        WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(btnDrag, mWindowParams);
    }

    // move the drag view
    private void drag(int rawX, int rawY)
    {
        if (btnDrag != null)
        {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) btnDrag.getLayoutParams();
            layoutParams.x = rawX - mDragPointOffsetX;
            layoutParams.y = rawY - mDragPointOffsetY;
            WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            mWindowManager.updateViewLayout(btnDrag, layoutParams);

            layoutParams = (WindowManager.LayoutParams) btnDrag.getLayoutParams();
            sendLocationToServer(layoutParams.x, layoutParams.y);
        }
    }

    private void sendLocationToServer(int viewX, int viewY)
    {
        //translate local point to remote
        float remoteWidth = config.width;
        float remoteHeight = config.height;
        float pointPercentX = viewX / screenWidth * 100;
        float pointPercentY = viewY / screenHeight * 100;
        viewX = (int) (pointPercentX * remoteWidth / 100);
        viewY = (int) (pointPercentY * remoteHeight / 100);
        try
        {
            String str = "<data>\nx=" + viewX + "\ny=" + viewY + "\n</data>";
            socketOutWriter.println(str);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {
        try
        {
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            socket = new Socket(serverAddr, SERVERPORT);
            socketOutWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            updateUi(true, null);
            CommunicationThread commThread = new CommunicationThread(socket);
            new Thread(commThread).start();
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
            updateUi(false, e1.getMessage());
        }
    }

    public void updateUi(final boolean connected, final String errorMessage)
    {
        uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if(connected)
                {
                    btnSend.setEnabled(true);
                    tvConnecting.setVisibility(View.GONE);
                }
                else
                {
                    tvConnecting.setText(errorMessage);
                    tvConnecting.setTextColor(Color.RED);
                }
            }
        });
    }

    @Override
    public void onClick(View v)
    {
        try
        {
            String str = etMessage.getText().toString();
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            out.println(str);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onTouch(View v, MotionEvent ev)
    {
        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN/* && x < this.getWidth() / 4*/)
        {
            mDragMode = true;

            rawXStart = (int) ev.getRawX();
            rawYStart = (int) ev.getRawY();
            mDragPointOffsetY = rawYStart - ((WindowManager.LayoutParams)btnDrag.getLayoutParams()).y;
            mDragPointOffsetX = rawXStart - ((WindowManager.LayoutParams)btnDrag.getLayoutParams()).x;
        }

        if (!mDragMode)
            return super.onTouchEvent(ev);

        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                drag((int)ev.getRawX(), (int)ev.getRawY());
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            default:
                mDragMode = false;
                //mEndPosition = pointToPosition(x, y);
                //stopDrag(mStartPosition - getFirstVisiblePosition());
                //if (mDragListener != null)
                //{
                 //   mDragListener.onDrop(ev.getRawX(), ev.getRawY(), mStartPosition);
                //}
                break;
        }
        return true;
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
                        data.type = Data.Type.valueOf(type);
                        continue;
                    }
                    if(Constants.DATA_END_TAG.equals(read))
                    {
                        if(data.isStarted())
                        {
                            onMessageReceived(data.close());
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

    public void onMessageReceived(final String message)
    {
        /*uiHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                //tvMessages.setText(tvMessages.getText().toString()+ read + "\n");
            }
        });*/
        System.out.println(message);
        config = new ServerConfig();
        String[] data = message.split("\n");
        for(String str : data)
        {
            if(str != null && str.startsWith("width"))
            {
                config.width = Utility.parseInt(str.split("=")[1], 0);//TODO defVal should be screen value
            }
            else if(str != null && str.startsWith("height"))
            {
                config.height = Utility.parseInt(str.split("=")[1], 0);
            }
            else if(str != null && str.startsWith("orientation"))
            {
                config.orientation = Utility.parseInt(str.split("=")[1], Configuration.ORIENTATION_UNDEFINED);
            }
        }
        setRequestedOrientation(Utility.convertConfigurationOrientationToActivityInfoOrientation(config.orientation));
    }
}
