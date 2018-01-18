package com.frost.mqtttutorial;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;
import java.util.List;

import helpers.MqttHelper;
import helpers.MqttHelper_publish;
import helpers.MqttHelper_second;

import static android.animation.ObjectAnimator.ofFloat;

public class MainActivity extends AppCompatActivity {

    int count_button_clicks;

    float h, w;
    float property_start;
    float propertyEnd;
    float translation_axis;
    String x_axis = "translationX";
    String y_axis = "translationY";
    String prev_axis;
    String propertyName = y_axis;
    boolean flag = false;
    final float wheel_radius = (float) 0.34;
    final float pi = (float) 3.14;
    private float x_coordinate;
    private float y_coordinate;
    private float velocity;
    private float acceleration;
    private float rpm;
    private float steering_angle;
    private float slip_angle;
    private float time;
    private float torque;
    private float force_longitudinal;
    float distance;
    int velocity_meters_min;
    float prevEnd_y;
    float prevEnd_x;
    int count=0;
    boolean motion;

    RelativeLayout playGround;
    ImageView image;
    ImageView car1;
    ImageView car2;
    ImageView car3;
    public Button btnAccelerateDecelerateInterpolator;
    public Button btnAccelerateInterpolator;
    public Button right;
    public Button left;
    public Button stop;
    public TextView t;
    ImageView steering;
    AccelerateDecelerateInterpolator accelerateDecelerateInterpolator;
    AccelerateInterpolator accelerateInterpolator;
    LinearInterpolator linearInterpolator;
    DecelerateInterpolator decelerateInterpolator;
    private double mCurrAngle = 0;
    private double mPrevAngle = 0;
    double xpos;
    double ypos;
    AnimatorSet as;

    String coordinates;


    MqttHelper_second mqttHelper_second;
    MqttHelper mqttHelper;
    MqttHelper_publish mqttpublish;
    TextView dataReceived;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playGround = (RelativeLayout) findViewById(R.id.playground);

        car1=(ImageView)findViewById(R.id.Car1);
        car2=(ImageView)findViewById(R.id.Car2);
        car3=(ImageView)findViewById(R.id.Car3);
        btnAccelerateInterpolator = (Button) findViewById(R.id.bAccelerateInterpolator);

        stop = (Button) findViewById(R.id.bDecelerateInterpolator);
        //t = (TextView) findViewById(R.id.textView);
        steering = (ImageView) findViewById(R.id.imageView2);

        dataReceived = (TextView) findViewById(R.id.textView);

        mqttpublish=new MqttHelper_publish(getApplicationContext());
        prepareInterpolator();
        startMqtt();
      //  mqttHelper = new MqttHelper(getApplicationContext());

        btnAccelerateInterpolator.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        prepareObjectAnimator(accelerateInterpolator);
                        break;

                    case MotionEvent.ACTION_UP:
                        car1.clearAnimation();
                        constant(linearInterpolator);
                        break;
                }
                return true;
            }

        });
        steering.setOnTouchListener(new View.OnTouchListener() {
            private Handler myHandler;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                final float xc = steering.getWidth() / 2;
                final float yc = steering.getHeight() / 2;

                final float x = motionEvent.getX();
                final float y = motionEvent.getY();

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        steering.clearAnimation();
                        mCurrAngle = Math.toDegrees(Math.atan2(x - xc, yc - y));
                        if (myHandler != null) {
                            return true;
                        }


                        break;

                    case MotionEvent.ACTION_MOVE:
                        count_button_clicks++;
                        mPrevAngle = mCurrAngle;
                        mCurrAngle = Math.toDegrees(Math.atan2(x - xc, yc - y));
                        car1.setRotation((float) mCurrAngle);
                        final RotateAnimation rotate = new RotateAnimation((float) mPrevAngle, (float) mCurrAngle,
                                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
                        rotate.setDuration(100);
                        rotate.setFillEnabled(true);
                        rotate.setFillAfter(true);
                        Log.d("Current Angle", mCurrAngle + "");
                        steering.startAnimation(rotate);
                        rotate.setDuration(100);
                        rotate.setFillEnabled(true);
                        rotate.setFillAfter(true);
                        Log.d("Current Angle", mCurrAngle + "");


                        if (myHandler != null) {
                            return true;
                        }

                        break;

                    case MotionEvent.ACTION_UP:
                        mPrevAngle = mCurrAngle = 0;
                        if (myHandler == null) {
                            return true;
                        }

                        break;
                }
                return true;

            }

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    float angle = car1.getRotation();
                    if (angle != mCurrAngle) {
                        car1.setRotation((float) angle + 30f);
                        angle = angle + 30f;

                    } else {


                        angle = 0;
                    }
                    myHandler.postDelayed(this, 500);
                }
            };
        });



        stop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                constant(linearInterpolator);
                return true;
            }
        });




    }

    private void startMqtt(){
        mqttHelper = new MqttHelper(getApplicationContext());
        mqttHelper_second=new MqttHelper_second(getApplicationContext());
        mqttHelper.mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("Debug","Connected");
            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Debug",mqttMessage.toString());
                if (topic.equals("hello/car2")) {
                    coordinates = mqttMessage.toString();
                    List<String> co = Arrays.asList(coordinates.split(","));
                    //for (int i = 0; i < 1000; i++) ;
                    if(Float.parseFloat(co.get(2))-car1.getX()<=10)
                    {
                        dataReceived.setText("Collision Detected");
                        halt();
                    }
                    car2.setTranslationX((Float.parseFloat(co.get(0))));
                    car2.setTranslationY((Float.parseFloat(co.get(1))));
                    // dataReceived.setText(mqttMessage.toString());
                    // mChart.addEntry(Float.valueOf(mqttMessage.toString()));
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        mqttHelper_second.mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.w("Debug","Connected");
            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                Log.w("Debug", topic);
                if (topic.equals("hello/car3")) {
                    coordinates = message.toString();
                    List<String> co = Arrays.asList(coordinates.split(","));
                    // for (int i = 0; i < 1000; i++) ;
                    if(Float.parseFloat(co.get(2))-car1.getX()<=100)
                    {
                        dataReceived.setText("Collision Detected");
                        halt();
                    }

                    car3.setTranslationX((Float.parseFloat(co.get(0))));
                    car3.setTranslationY((Float.parseFloat(co.get(1))));
                    // dataReceived.setText(mqttMessage.toString());
                    // mChart.addEntry(Float.valueOf(mqttMessage.toString()));
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }



    private void prepareInterpolator() {
        accelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
        accelerateInterpolator = new AccelerateInterpolator();
        linearInterpolator = new LinearInterpolator();
        decelerateInterpolator = new DecelerateInterpolator();
        prevEnd_y = (-(h - (float) car1.getHeight() / 2 - 100));
    }

    private void prepareObjectAnimator(TimeInterpolator timeInterpolator) {
        w = (float) playGround.getWidth();
        h = (float) playGround.getHeight();

        if(count==0)
        {
            if(car1.getTranslationY()<=-(h - (float) car1.getHeight() / 2 - 100)+50) {
                count = 1;
            }
            else
            {
                propertyName = y_axis;
                property_start = car1.getTranslationY();
                propertyEnd = -(h - (float) car1.getHeight() / 2 - 100);
                Log.w("Debug",car2.getTranslationX()+"");
            }

        }
        if(count==1)
        {
            if(car1.getTranslationX()>= 638) {
                count = 2;
            }
            else
            {
                propertyName=x_axis;
                property_start = car1.getTranslationX();
                propertyEnd = 638;
            }
        }
        if(count==2)
        {
            if(car1.getTranslationY()>=-50) {
                count = 3;
            }
            else
            {
                propertyName = y_axis;
                property_start = car1.getTranslationY();
                propertyEnd = 0;
            }
        }
        if(count ==3)
        {
            if(car1.getTranslationX()<=(50)) {
                count = 0;
                propertyName = y_axis;
                property_start = car1.getTranslationY();
                propertyEnd = -(h - (float) car1.getHeight() / 2 - 100);
            }
            else
            {
                propertyName=x_axis;
                property_start = car1.getTranslationX();
                propertyEnd = 0;
            }
        }


        final ObjectAnimator objectAnimator = ofFloat(car1, propertyName, property_start, propertyEnd);

        if (timeInterpolator instanceof AccelerateInterpolator) {
            objectAnimator.setDuration(5000);
            objectAnimator.setInterpolator(timeInterpolator);
            prev_axis = propertyName;
            objectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    time = objectAnimator.getCurrentPlayTime();
                    distance = (float) Math.sqrt((car1.getTranslationX() * car1.getTranslationX()) + (car1.getTranslationY() * car1.getTranslationY()));
                    velocity = distance / time;
                    acceleration = velocity / time;
                    velocity_meters_min = (int) (velocity * 100);
                    rpm = (int) (velocity_meters_min / (2 * pi * wheel_radius));
                    if (rpm > 100 && rpm < 200) {
                        torque = 325;
                    } else if (rpm >= 200 && rpm < 300) {
                        torque = 330;
                    } else if (rpm >= 300 && rpm < 400) {
                        torque = 350;
                    } else if (rpm >= 400 && rpm < 500) {
                        torque = 350;

                    } else if (rpm >= 500 && rpm < 600) {
                        torque = 325;
                    } else {
                        torque = 0;
                    }
                    dataReceived.setText("X ="+car1.getTranslationX());
                 /*   t.setText("Time Elapsed :" + objectAnimator.getCurrentPlayTime() + "\nX axis :" + image.getTranslationX() +
                            "\nY_axis :" + image.getTranslationY() + "\nVelocity :" + velocity + "\nAcceleration :" + acceleration
                            + "\nRpm :" + rpm + "\nTorque :" + torque);*/
                    mqttpublish.publish("hello/car1", car1.getTranslationX() + "," + car1.getTranslationY()+","+car1.getX());
                }
            });
            objectAnimator.start();
        } else if (timeInterpolator instanceof AccelerateDecelerateInterpolator) {
            objectAnimator.setDuration(5000);
            objectAnimator.setInterpolator(timeInterpolator);
            objectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    time = objectAnimator.getCurrentPlayTime();
                    distance = (float) Math.sqrt((car1.getTranslationX() * car1.getTranslationX()) + (car1.getTranslationY() * car1.getTranslationY()));
                    velocity = distance / time;
                    acceleration = velocity / time;
                    velocity_meters_min = (int) (velocity * 60);
                    rpm = (int) (velocity_meters_min / (2 * pi * wheel_radius));
                    if (rpm > 1 && rpm < 2) {
                        torque = 325;
                    } else if (rpm >= 2 && rpm < 3) {
                        torque = 330;
                    } else if (rpm >= 3 && rpm < 4) {
                        torque = 350;
                    } else if (rpm >= 4 && rpm < 5) {
                        torque = 350;

                    } else if (rpm >= 5 && rpm < 6) {
                        torque = 325;
                    } else {
                        torque = 0;
                    }
                  /*  t.setText("Time Elapsed :" + objectAnimator.getCurrentPlayTime() + "\nX axis :" + image.getTranslationX() +
                            "\nY_axis :" + image.getTranslationY() + "\nVelocity :" + velocity + "\nAcceleration :" + acceleration
                            + "\nRpm :" + rpm + "\nTorque :" + torque); */
                  //  mqttAndroidClient.publish("hello/car1", data.getBytes(), 0, false);
                    mqttpublish.publish("hello/car1" ,car1.getTranslationX()+","+car1.getTranslationY()+","+car1.getX());
                }
            });
            objectAnimator.start();
        }

    }



    public void constant(TimeInterpolator timeInterpolator) {

        if (count == 0) {
            if (car1.getTranslationY() <= -(h - (float) car1.getHeight() / 2 - 100) + 50) {
                motion = false;
            } else {
                propertyName = y_axis;
                property_start = car1.getTranslationY();
                propertyEnd = car1.getTranslationY() - 80f;
                motion = true;
            }

        }
        if (count == 1) {
            if (car1.getTranslationX() >= 638) {
                motion = false;
            } else {
                propertyName = x_axis;
                property_start = car1.getTranslationX();
                propertyEnd = car1.getTranslationX() + 80f;
                motion = true;
            }
        }
        if (count == 2) {
            if (car1.getTranslationY() >= -50) {
                motion = false;
            } else {
                propertyName = y_axis;
                property_start = car1.getTranslationY();
                propertyEnd = car1.getTranslationY() + 80f;
                motion = true;
            }
        }
        if (count == 3) {
            if (car1.getTranslationX() <= (50)) {
                motion = false;
            } else {
                propertyName = x_axis;
                property_start = car1.getTranslationX();
                propertyEnd = car1.getTranslationX() - 80f;
                motion = true;
            }
        }


        final ObjectAnimator objectAnimator = ofFloat(car1, propertyName, property_start, propertyEnd);
        objectAnimator.setDuration(5000);
        objectAnimator.setInterpolator(timeInterpolator);
        objectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                time = objectAnimator.getCurrentPlayTime();
                distance = (float) Math.sqrt((car1.getTranslationX() * car1.getTranslationX()) + (car1.getTranslationY() * car1.getTranslationY()));
                velocity = distance / time;
                acceleration = velocity / time;
                velocity_meters_min = (int) (velocity * 100);
                rpm = (int) (velocity_meters_min / (2 * pi * wheel_radius));
                if (rpm > 100 && rpm < 200) {
                    torque = 325;
                } else if (rpm >= 200 && rpm < 300) {
                    torque = 330;
                } else if (rpm >= 300 && rpm < 400) {
                    torque = 350;
                } else if (rpm >= 400 && rpm < 500) {
                    torque = 350;

                } else if (rpm >= 500 && rpm < 600) {
                    torque = 325;
                } else {
                    torque = 0;
                }
                dataReceived.setText("X =" + car1.getTranslationX());
               /* t.setText("Time Elapsed :" + objectAnimator.getCurrentPlayTime() + "\nX axis :" + image.getTranslationX() +
                        "\nY_axis :" + image.getTranslationY() + "\nVelocity :" + velocity + "\nAcceleration :" + acceleration
                        + "\nRpm :" + rpm + "\nTorque :" + torque);*/
                mqttpublish.publish("hello/car1",car1.getTranslationX() + "," + car1.getTranslationY()+","+car1.getX());
            }
        });
        if (motion) {
            objectAnimator.start();
        }
    }

    public void halt()
    {
        car1.clearAnimation();
       if(propertyName==x_axis)
       {
           final ObjectAnimator objectAnimator = ofFloat(car1, propertyName, car1.getTranslationX(),car1.getTranslationX()+10f);
           objectAnimator.setDuration(5000);
           objectAnimator.setInterpolator(linearInterpolator);
           objectAnimator.start();
       }
       else if(propertyName==y_axis)
       {
           final ObjectAnimator objectAnimator = ofFloat(car1, propertyName, car1.getTranslationY(),car1.getTranslationY()+10f);
           objectAnimator.setDuration(5000);
           objectAnimator.setInterpolator(linearInterpolator);
           objectAnimator.start();
       }
    }
}
