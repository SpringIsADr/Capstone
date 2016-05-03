package com.google.vrtoolkit.cardboard.samples.simplevideowidget;


        import android.app.Activity;
        import android.content.Intent;
        import android.os.Bundle;
        import android.view.View;
        import android.widget.Button;
        import android.content.ComponentName;


        import com.google.vrtoolkit.cardboard.samples.simplevideowidget.R;


public class MainActivity extends Activity {

    Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        button=(Button) findViewById(R.id.butn1);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Intent intent= new Intent(MainActivity.this,SimpleVrVideoActivity.class);
                // startActivity(intent);

                ComponentName componetName = new ComponentName(

                        "com.google.vrtoolkit.cardboard.samples.simplevideowidget",

                        "com.google.vrtoolkit.cardboard.samples.simplevideowidget.SimpleVrVideoActivity");

                try {
                    Intent intent = new Intent();
                    intent.setComponent(componetName);
                    startActivity(intent);
                } catch (Exception e) {


                }

            }
        });
        button=(Button) findViewById(R.id.butn2);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                ComponentName componetName = new ComponentName(

                        "com.google.vrtoolkit.cardboard.samples.treasurehunt",

                        "com.google.vrtoolkit.cardboard.samples.treasurehunt.TreasureHuntActivity");

                try {
                    Intent intent = new Intent();
                    intent.setComponent(componetName);
                    startActivity(intent);
                } catch (Exception e) {


                }


            }
        });

        button=(Button) findViewById(R.id.butn3);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                ComponentName componetName = new ComponentName(

                        "com.luluapp.lulu_home.vrvideoplayer",

                        "com.luluapp.lulu_home.vrvideoplayer.MainActivity");

                try {
                    Intent intent = new Intent();
                    intent.setComponent(componetName);
                    startActivity(intent);
                } catch (Exception e) {


                }


            }
        });

        button=(Button) findViewById(R.id.butn4);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                ComponentName componetName = new ComponentName(

                        "com.luluapp.lulu_home.webview",

                        "com.luluapp.lulu_home.webview.MainActivity");

                try {
                    Intent intent = new Intent();
                    intent.setComponent(componetName);
                    startActivity(intent);
                } catch (Exception e) {


                }


            }
        });

    }


    public void directAR(View view) {
    }




    public void directGame(View view) {
    }


    public void directRealtime(View view) {
    }

    public void directAR2(View view) {
    }
}

