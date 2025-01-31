package com.example.murtaza.walkietalkie;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class ChatWindow extends AppCompatActivity implements View.OnClickListener {

    Button send_btn;
    private static final int MESSAGE_READ = 1;
    private static boolean isRecording = false;
    private MicRecorder micRecorder;
    OutputStream outputStream;
    Thread t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_window);

        send_btn = (Button)findViewById(R.id.send_file_btn);
        send_btn.setOnClickListener(this);

        Socket socket = SocketHandler.getSocket();

        try {
            outputStream = socket.getOutputStream();
            Log.e("OUTPUT_SOCKET", "SUCCESS");
            startService(new Intent(getApplicationContext(), AudioStreamingService.class));


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.send_file_btn:
                if(send_btn.getText().toString().equals("TALK")){
                    // stream audio
                    send_btn.setText("OVER");
                    micRecorder = new MicRecorder();
                    t = new Thread(micRecorder);
                    if(micRecorder != null) {
                        MicRecorder.keepRecording = true;
                    }
                    t.start();


                }else if(send_btn.getText().toString().equals("OVER")){
                    send_btn.setText("TALK");
                    if(micRecorder != null) {
                        MicRecorder.keepRecording = false;
                    }

                }

                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(micRecorder != null) {
            MicRecorder.keepRecording = false;
        }
    }
}
