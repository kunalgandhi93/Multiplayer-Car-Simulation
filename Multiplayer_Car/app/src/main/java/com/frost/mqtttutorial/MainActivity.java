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


    //---------------------------------------below start all Non-UI var ---------------------- //
    int count_button_clicks;
    float h, w;

    //these variables set the start and end point of Car motion
    float property_start;
    float propertyEnd;

    String x_axis = "translationX";
    String y_axis = "translationY";

    //this variable defines Car Direction
    String propertyName = y_axis;

    final float wheel_radius = (float) 0.34;
    final float pi = (float) 3.14;

    /*These variables are used to store position, velocity,
      acceleration,rpm of the car*/
    private float x_coordinate;
    private float y_coordinate;
    private float velocity;
    private float acceleration;
    private float rpm;
    private float time;
    private float torque;
    float distance;
    int velocity_meters_min;

    //count is used to change direction of Car motion
    int count=0;

    boolean motion;

    //coordinate stores x and y position of other Cars on the network
    String coordinates;

    private double mCurrAngle = 0;
    private double mPrevAngle = 0;
    double xpos;
    double ypos;

//--------------------------------------- End of Non-UI var ---------------------------- //


    //---------------------------------------below start UI-variables ---------------------- //
    RelativeLayout playGround;
    ImageView car1;
    ImageView car2;
    ImageView car3;
    ImageView steering; // Steering of the Car
    public Button btnAccelerateInterpolator; //Accelerator Button
    public Button stop;  // Brake Button
    public TextView dataReceived; //
    AnimatorSet as;
//--------------------------------------- End of UI variables ---------------------------- //

    //------------------Declaration of different Animation Instances start Here---------------- //
    AccelerateDecelerateInterpolator accelerateDecelerateInterpolator;
    AccelerateInterpolator accelerateInterpolator;
    LinearInterpolator linearInterpolator;
    DecelerateInterpolator decelerateInterpolator;
//--------------------------------------- End of Declaration-------------------------------- //

    //------------------Declaration of different Instances of MQTT protocol--------------------- //
	/*These instance subscribe to other Raspberry Pi to read co-ordinate of
	  other cars in the network*/
    MqttHelper_second mqttHelper_second;
    MqttHelper mqttHelper;

    //This instance publish data i.e. co-ordinate of its own Car on Raspberry Pi
    MqttHelper_publish mqttpublish;
//--------------------------------------- End of Declaration-------------------------------- //

    //----------------below start all operation function of this activity ---------------------- //
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initializing UI variables with particular IDs.
        playGround = (RelativeLayout) findViewById(R.id.playground);
        car1=(ImageView)findViewById(R.id.Car1);
        car2=(ImageView)findViewById(R.id.Car2);
        car3=(ImageView)findViewById(R.id.Car3);
        btnAccelerateInterpolator = (Button) findViewById(R.id.bAccelerateInterpolator);
        stop = (Button) findViewById(R.id.bDecelerateInterpolator);
        steering = (ImageView) findViewById(R.id.imageView2);
        dataReceived = (TextView) findViewById(R.id.textView);

        mqttpublish=new MqttHelper_publish(getApplicationContext());
        prepareInterpolator();
        startMqtt();

        btnAccelerateInterpolator.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        prepareObjectAnimator(accelerateInterpolator);
                        break;

                    case MotionEvent.ACTION_UP:
                        car2.clearAnimation();
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
                        car2.setRotation((float) mCurrAngle);
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
                    float angle = car2.getRotation();
                    if (angle != mCurrAngle) {
                        car2.setRotation((float) angle + 30f);
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

    /**
     * Function Name : startMqtt()
     * This method sets the movement of other two cars on the network and
     * detects collision between Cars
     */
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
                if (topic.equals("hello/car1")) {
                    coordinates = mqttMessage.toString();
                    List<String> co = Arrays.asList(coordinates.split(","));
                    if(Float.parseFloat(co.get(2))-car2.getX()<=100 && propertyName==x_axis)
                    {
                        dataReceived.setText("Collision Detected");
                        halt();
                    }
                    else
                    {
                        dataReceived.setText("");
                    }
                    car1.setTranslationX((Float.parseFloat(co.get(0))));
                    car1.setTranslationY((Float.parseFloat(co.get(1))));
                    car1.setRotation(Float.parseFloat(co.get(2)));
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
                    //for (int i = 0; i < 1000; i++) ;
                    if(Float.parseFloat(co.get(2))-car2.getX()<=100 && propertyName==x_axis)
                    {
                        dataReceived.setText("Collision Detected");
                        halt();
                    }
                    else
                    {
                        dataReceived.setText("");
                    }
                    car3.setTranslationX((Float.parseFloat(co.get(0))));
                    car3.setTranslationY((Float.parseFloat(co.get(1))));
                    car3.setRotation(Float.parseFloat(co.get(2)));
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }


    /**
     * Function Name : prepareInterpolator()
     * This method is used to create animation instances for Car Image
     */
    private void prepareInterpolator() {
        accelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
        accelerateInterpolator = new AccelerateInterpolator();
        linearInterpolator = new LinearInterpolator();
        decelerateInterpolator = new DecelerateInterpolator();
    }

    /**
     * Function Name : prepareObjectAnimator()
     * Argument : TimeInterpolator
     * This method checks the instance of the animator and accordingly it animates the Car Image
     */
    private void prepareObjectAnimator(TimeInterpolator timeInterpolator) {
        w = (float) playGround.getWidth();
        h = (float) playGround.getHeight();

        if(count==0)
        {
            if(car2.getTranslationY()<=-(h - (float) car2.getHeight() / 2 - 100)+50) {
                count = 1;
            }
            else
            {
                propertyName = y_axis;
                property_start = car2.getTranslationY();
                propertyEnd = -(h - (float) car2.getHeight() / 2 - 100);
            }

        }
        if(count==1)
        {
            if(car2.getTranslationX()>= 780) {
                count = 2;
            }
            else
            {
                propertyName=x_axis;
                property_start = car2.getTranslationX();
                propertyEnd = 780;
            }
        }
        if(count==2)
        {
            if(car2.getTranslationY()>=-50) {
                count = 3;
            }
            else
            {
                propertyName = y_axis;
                property_start = car2.getTranslationY();
                propertyEnd = 0;
            }
        }
        if(count ==3)
        {
            if(car2.getTranslationX()<=(50)) {
                count = 0;
                propertyName = y_axis;
                property_start = car2.getTranslationY();
                propertyEnd = -(h - (float) car2.getHeight() / 2 - 100);
            }
            else
            {
                propertyName=x_axis;
                property_start = car2.getTranslationX();
                propertyEnd = 0;
            }
        }

        final ObjectAnimator objectAnimator = ofFloat(car2, propertyName, property_start, propertyEnd);

        if (timeInterpolator instanceof AccelerateInterpolator) {
            objectAnimator.setDuration(5000);
            objectAnimator.setInterpolator(timeInterpolator);
            objectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    time = objectAnimator.getCurrentPlayTime();
                    distance = (float) Math.sqrt((car2.getTranslationX() * car2.getTranslationX()) + (car2.getTranslationY() * car2.getTranslationY()));
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
                    dataReceived.setText("Time Elapsed :" + objectAnimator.getCurrentPlayTime() + "\nX axis :" + car2.getTranslationX() +
                            "\nY_axis :" + car2.getTranslationY() + "\nVelocity :" + velocity + "\nAcceleration :" + acceleration
                            + "\nRpm :" + rpm + "\nTorque :" + torque);
                    mqttpublish.publish("hello/car2" ,car2.getTranslationX()+","+car2.getTranslationY()+","+car2.getX()+","+ car2.getRotation());
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
                    distance = (float) Math.sqrt((car2.getTranslationX() * car2.getTranslationX()) + (car2.getTranslationY() * car2.getTranslationY()));
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
                    dataReceived.setText("Time Elapsed :" + objectAnimator.getCurrentPlayTime() + "\nX axis :" + car2.getTranslationX() +
                            "\nY_axis :" + car2.getTranslationY() + "\nVelocity :" + velocity + "\nAcceleration :" + acceleration
                            + "\nRpm :" + rpm + "\nTorque :" + torque);
                    mqttpublish.publish("hello/car2" ,car2.getTranslationX()+","+car2.getTranslationY()+","+car2.getX()+","+ car2.getRotation());
                }
            });
            objectAnimator.start();
        }

    }


    /**
     * Function Name : constant()
     * Argument : TimeInterpolator
     * This method decelerate the speed of Car whenever brake button is press
     */
    public void constant(TimeInterpolator timeInterpolator) {

        if(count==0)
        {
            if(car2.getTranslationY()<=-(h - (float) car2.getHeight() / 2 - 100)+50) {
                motion=false;
            }
            else
            {
                propertyName = y_axis;
                property_start = car2.getTranslationY();
                propertyEnd = car2.getTranslationY() - 80f;
                motion=true;
            }

        }
        if(count==1)
        {
            if(car2.getTranslationX()>= 780) {
                motion=false;
            }
            else
            {
                propertyName=x_axis;
                property_start = car2.getTranslationX();
                propertyEnd = car2.getTranslationX() + 80f;
                motion=true;
            }
        }
        if(count==2)
        {
            if(car2.getTranslationY()>=-50) {
                motion=false;
            }
            else
            {
                propertyName = y_axis;
                property_start = car2.getTranslationY();
                propertyEnd = car2.getTranslationY() + 80f;
                motion=true;
            }
        }
        if(count ==3)
        {
            if(car2.getTranslationX()<=(50)) {
                motion=false;
            }
            else
            {
                propertyName=x_axis;
                property_start = car2.getTranslationX();
                propertyEnd = car2.getTranslationX() - 80f;
                motion=true;
            }
        }

        final ObjectAnimator objectAnimator = ofFloat(car2, propertyName, property_start, propertyEnd);
        objectAnimator.setDuration(5000);
        objectAnimator.setInterpolator(timeInterpolator);
        objectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                time = objectAnimator.getCurrentPlayTime();
                distance = (float) Math.sqrt((car2.getTranslationX() * car2.getTranslationX()) + (car2.getTranslationY() * car2.getTranslationY()));
                velocity = distance / time;
                acceleration = velocity / time;
                velocity_meters_min = (int) (velocity * 100);
                rpm = (int) (velocity_meters_min / (2 * pi * wheel_radius));
                if (rpm > 100&& rpm < 200) {
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
                dataReceived.setText("Time Elapsed :" + objectAnimator.getCurrentPlayTime() + "\nX axis :" + car2.getTranslationX() +
                        "\nY_axis :" + car2.getTranslationY() + "\nVelocity :" + velocity + "\nAcceleration :" + acceleration
                        + "\nRpm :" + rpm + "\nTorque :" + torque);
                mqttpublish.publish("hello/car2" ,car2.getTranslationX()+","+car2.getTranslationY()+","+car2.getX()+","+ car2.getRotation());

            }
        });

        if(motion) {
            objectAnimator.start();
        }
    }

    /**
     * Function Name : halt()
     * This method stops motion of Car whenever collision is detected
     */
    public void halt()
    {
        car2.clearAnimation();
        if(propertyName==x_axis)
        {
            final ObjectAnimator objectAnimator = ofFloat(car2, propertyName, car2.getTranslationX(),car2.getTranslationX()+10f);
            objectAnimator.setDuration(5000);
            objectAnimator.setInterpolator(linearInterpolator);
            objectAnimator.start();
        }
        else if(propertyName==y_axis)
        {
            final ObjectAnimator objectAnimator = ofFloat(car2, propertyName, car2.getTranslationY(),car2.getTranslationY()+10f);
            objectAnimator.setDuration(5000);
            objectAnimator.setInterpolator(linearInterpolator);
            objectAnimator.start();
        }
        mqttpublish.publish("hello/car2" ,car2.getTranslationX()+","+car2.getTranslationY()+","+car2.getX()+","+ car2.getRotation());
    }


}
