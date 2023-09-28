package com.example.murtaza.walkietalkie;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class mainpage_ui_test extends AppCompatActivity implements View.OnClickListener {

    private ImageView foundDevice;
    static int count = 0;
    ArrayList<Integer> device_ids = new ArrayList<>();
    ArrayList<TextView> device_txt_view = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage_ui_test);

        final Handler handler=new Handler();

        View device1 = createNewDevice();
        View device2 = createNewDevice();


        final View[] device_array = {device1, device2};

//        foundDevice=(ImageView)findViewById(R.id.foundDevice);
        ImageView button=(ImageView)findViewById(R.id.discoverView);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        int index = count % device_array.length;
                        foundDevice(device_array[index]);
                        count++;
//                        foundDevice(device1);
                    }
                },1000);
            }
        });
    }

    public View createNewDevice(){
        View device1 = LayoutInflater.from(this).inflate(R.layout.device_icon, null);

        TextView txt_device1 = device1.findViewById(R.id.myImageViewText);
        int device_id = (int)(Math.random()*1000);
        txt_device1.setText(device_id+"");
        device1.setId(device_id);
        device1.setOnClickListener(this);

        device_txt_view.add(txt_device1);
        device_ids.add(device_id);
        return device1;
    }

    private void foundDevice(View foundDevice){
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(400);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        ArrayList<Animator> animatorList=new ArrayList<Animator>();
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleX", 0f, 1.2f, 1f);
        animatorList.add(scaleXAnimator);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleY", 0f, 1.2f, 1f);
        animatorList.add(scaleYAnimator);
        animatorSet.playTogether(animatorList);
        foundDevice.setVisibility(View.VISIBLE);
        animatorSet.start();
    }

    @Override
    public void onClick(View v) {
        int view_id = v.getId();
        if(device_ids.contains(view_id)){
            int idx = device_ids.indexOf(view_id);
            Toast.makeText(getApplicationContext(), idx+" Clicked", Toast.LENGTH_SHORT).show();
        }
    }
}
