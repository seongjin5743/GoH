import serial
import time

arduino = serial.Serial(port='COM7', baudrate=9600, timeout=1)
cnt = 0
while True:
    if cnt < 10:
        arduino.write(b'1')
        time.sleep(1)
        cnt += 1
    else:
        arduino.write(b'0')
        time.sleep(1)