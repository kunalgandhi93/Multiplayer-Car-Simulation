# Multiplayer-Car-Simulation

In this Code,I have implemented MQTT protocol to communicate between Android Device and Raspberry Pi. In this protocol the android devices are publisher as well as subscribers and the Raspberry Pis are brokers. Each Android device subscribes to another two Raspberry Pis and publishes its parameters to its own Raspberry Pi. This method is then implemented on the other two android devices as well.

MainActivity File location : Multiplayer_Car/app/src/main/java/com/frost/mqtttutorial/MainActivity.java

Installation:
1) Install Eclipse-Paho on Raspberry Pi.

2) Install Mosquitto on Raspberry Pi.

Instructions needed to turn ON Raspberry Pi to start broker:
mosquitto_sub –h localhost –t “<Topic name>”

References:
1) https://wildanmsyah.wordpress.com/2017/05/11/mqtt-android-client-tutorial/
2) https://diyhacking.com/connecting-raspberry-pi-iot-devices-mqtt/
3) https://github.com/eclipse/paho.mqtt.java/tree/master/org.eclipse.paho.client.mqttv3
4) http://www.instructables.com/id/Installing-MQTT-BrokerMosquitto-on-Raspberry-Pi/

Note :

Please change Image id while installing this application on Multiple Devices and also modify topics name accordingly.

