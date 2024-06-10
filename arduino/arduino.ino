#include <Servo.h>

Servo myServo;
int button = 8;
int servoPin = 9;

void setup() {
  Serial.begin(9600);
  myServo.attach(servoPin);
  pinMode(button, INPUT_PULLUP);
  myServo.write(90);
}

void loop() {
  if (Serial.available() > 0) { 
    char input = Serial.read(); 
    if (input == '0') {
      myServo.write(90);
    } else if (input == '1') {
      myServo.write(0);
    }
  }

  int buttonState = digitalRead(button);
  if (buttonState == LOW) {
    myServo.write(0);
    delay(10000);
    myServo.write(90);
  }
}