# PhonePad

Android app and linux script for controlling desktop mouse pointer by touchscreen on 
smartphone.

## Compilation
1. Install xdev headers
```
sudo apt-get install libx11-dev
```

2. Compile phone-pad.c
```
gcc phone-pad.c -o phone-pad -lX11
```

## Usage
1. Open PhonePad app, check mobile IP by clicking on info icon.

2. Start phone-pad script on your machine
```
./phone-pad -a 192.168.1.100 -p 8000
```