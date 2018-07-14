package com.example.ankushw.leaphackathon;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
    }

    public void yesB(View view){
        Intent newIntent= new Intent(SplashActivity.this, MainActivity.class);
        newIntent.putExtra("selection","yes");
        startActivity(newIntent);
    }

    public void noB(View view){
        Intent newIntent= new Intent(SplashActivity.this, MainActivity.class);
        newIntent.putExtra("selection","no");
        startActivity(newIntent);
    }
}
