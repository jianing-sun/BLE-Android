# BLE-Android
This repository contains three applications I made about Bluetooth Low Energy (BLE) with Android. These apps are similar but with different purposes or devices. IDE is Android Studio, and BLE devices are Sensortag from TI, and BlueNRG from STMicroelectronics.

- Blue_NRG: An application based on BlueNRG from STM and could used to find, connect, and receive data from a basic program in Keil for this board. Once you press the blue button, the data stream (hex string) will increase by 1 and these data will appear in the app. This app was created for senior students from ECSE426 in McGill as part of their final project.
- mySensorTag: Thie app based on SensorTag CC2650 from Texas Instruments. It can find, connect, receive accelerometer data of x, y, z axes. I transferred these accelerometer byte array raw value to real accelerate value in these three directions and based on these values, I put a simple algorithm and math inside so as to calculate my steps. The pedometer part is derived from one of my past pedometer apps but this time I didn't use the step counter inside Android device but used the accelerometer data from SensorTag. The accuracy is kind of a problem for the pedometer but I just regard it as a good example or hints about how to make fully use of the data from environment sensors as these kind of sensors are too prosaic to think about what can I use it for sometimes.
- SensorTag&Firebase: This is simply plus Firebase Realtime Database for the second one. I used it to test how to upload data to Firebase (only on way now, not include the retrieve part yet). For the login page, I enabled email as the authentication and I create a FAKE user in my Console of Firebase — jianing.sun (at) gmail.com, the password is 950621. The address is NOT me and the password is fake. It is just a visual user in my Firebase used to receive data. After the login view of the app, it will jump to the MainActivity which contains scan fragment and device fragment exactly the same from the second one.

Some source code credit to @j4velin and [EazeGraphLibrary](https://github.com/blackfizz/EazeGraph).  

*Each app is associated with a pdf or markdown report. Blue_NRG is a demo for students so I uploaded along with a handout which is used for my tutorial.*

    
      
      

**SUNJIANING**   
**Organize these BLE related apps in March 2018  (Winter 2018)**
