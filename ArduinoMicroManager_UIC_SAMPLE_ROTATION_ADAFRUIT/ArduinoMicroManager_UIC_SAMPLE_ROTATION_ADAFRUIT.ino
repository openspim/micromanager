/*
 * This goal of the application is to set the digital output on pins 8-13 
 * This can be accomplished in three ways.  First, a serial command can directly set
 * the digital output pattern.  Second, a series of patterns can be stored in the 
 * Arduino and TTLs coming in on pin 2 will then trigger to the consecutive pattern (trigger mode).
 * Third, intervals between consecutive patterns can be specified and paterns will be 
 * generated at these specified time points (timed trigger mode).
 *
 * Interface specifications:
 * digital pattern specification: single byte, bit 0 corresponds to pin 8, 
 *   bit 1 to pin 9, etc..  Bits 7 and 8 will not be used (and should stay 0).
 *
 * Set digital output command: 1p
 *   Where p is the desired digital pattern.  Controller will return 1 to 
 *   indicate succesfull execution.
 *
 * Get digital output command: 2
 *   Controller will return 2p.  Where p is the current digital output pattern
 *
 * Set Analogue output command: 3xvv
 *   Where x is the output channel (either 1 or 2), and vv is the output in a 
 *   12-bit significant number.
 *   Controller will return 3xvv:
 *
 * Get Analogue output:  4
 *
 *
 * Set digital patten for triggered mode: 5xd 
 *   Where x is the number of the pattern (currently, 12 patterns can be stored).
 *   and d is the digital pattern to be stored at that position.  Note that x should
 *   be the real number (i.e., not  ASCI encoded)
 *   Controller will return 5xd 
 *
 * Set the Number of digital patterns to be used: 6x
 *   Where x indicates how many digital patterns will be used (currently, up to 12
 *   patterns maximum).  In triggered mode, after reaching this many triggers, 
 *   the controller will re-start the sequence with the first pattern.
 *   Controller will return 6x
 *
 * Skip trigger: 7x
 *   Where x indicates how many digital change events on the trigger input pin
 *   will be ignored.
 *   Controller will respond with 7x
 *
 * Start trigger mode: 8
 *   Controller will return 8 to indicate start of triggered mode
 *   Stop triggered mode by sending any key (including new commands, that will be 
 *   processed).  Trigger mode will  stop blanking mode (if it was active)
 * 
 * Get result of Trigger mode: 9
 *   Controller will return 9x where x is the number of triggers received during the last
 *   trigger mode run
 *
 * Set time interval for timed trigger mode: 10xtt
 *   Where x is the number of the interval (currently, 12 intervals can be stored)
 *   and tt is the interval (in ms) in Arduino unsigned int format.  
 *   Controller will return 10x
 *
  * Sets how often the timed pattern will be repeated: 11x
 *   This value will be used in timed-trigger mode and sets how often the output
 *   pattern will be repeated. 
 *   Controller will return 11x
 *  
 * Starts timed trigger mode: 12
 *   In timed trigger mode, digital patterns as set with function 5 will appear on the 
 *   output pins with intervals (in ms) as set with function 10.  After the number of 
 *   patterns set with function 6, the pattern will be repeated for the number of times
 *   set with function 11.  Any input character (which will be processed) will stop 
 *   the pattern generation.
 *   Controller will retun 12.
 * 
 * Start blanking Mode: 20
 *   In blanking mode, zeroes will be written on the output pins when the trigger pin
 *   is low, when the trigger pin is high, the pattern set with command #1 will be 
 *   applied to the output pins. 
 *   Controller will return 20
 *
 * Stop blanking Mode: 21
 *   Stops blanking mode.  Controller returns 21
 *
 * Blanking mode trigger direction: 22x
 *   Sets whether to blank on trigger high or trigger low.  x=0: blank on trigger high,
 *   x=1: blank on trigger low.  x=0 is the default
 *   Controller returns 22
 *
 * 
 * Get Identification: 30
 *   Returns (asci!) MM-Ard\r\n
 *
 * Get Version: 31
 *   Returns: version number (as ASCI string) \r\n
 *
 * Read digital state of analogue input pins 0-5: 40
 *   Returns raw value of PINC (two high bits are not used)
 *
 * Read analogue state of pint pins 0-5: 41x
 *   x=0-5.  Returns analogue value as a 10-bit number (0-1023)
 *
 *
 * 
 * Possible extensions:
 *   Set and Get Mode (low, change, rising, falling) for trigger mode
 *   Get digital patterm
 *   Get Number of digital patterns
 */
 
  #include <AFMotor.h> //to include the Adafruit Motor Shield library
  AF_Stepper motor1(200, 1); //defines class motor as 200 steps motor working on M1 and M2
  AF_Stepper motor2(200, 2); //defines class motor as 200 steps motor working on M3 and M4
  int currentFilter1 = 1; //filter on the wheel 1
  int currentFilter2 = 1; //filter on the wheel 2
  int totalfilterChanges1 = 0;
  int totalfilterChanges2 = 0;  
  
  int current_position=0;
  int current_position2=0;
  int x=0;
  int mov=0; 
  
   unsigned int version_ = 2;
   
   // pin on which to receive the trigger (either 2 or 3, changed attachInterrupt accordingly)
   int inPin_ = 2;  
   // pin connected to DIN of TLV5618
   int dataPin = 3;
   // pin connected to SCLK of TLV5618
   int clockPin = 4;
   // pin connected to CS of TLV5618
   int latchPin = 5;
   // pin connected to LDAC
   int LDACPin = 6;

   int aP1 = 11; // A positive
   int bP2 = 10; // B positive
   int aN3 = 9; // A negative
   int bN4 = 8; // B negative
   int previous = 4;
   int clockwise = 1;
   int delayTime = 4;
   int stepsFinal = 0;
   int steps = 0;
   int inNumber = 0;
   int i = 0;
   char charRead = '0';
   int intRead = 0;

   const int SEQUENCELENGTH = 12;  // this should be good enough for everybody;)
   byte triggerPattern_[SEQUENCELENGTH] = {0,0,0,0,0,0,0,0,0,0,0,0};
   unsigned int triggerDelay_[SEQUENCELENGTH] = {0,0,0,0,0,0,0,0,0,0,0,0};
   int patternLength_ = 0;
   byte repeatPattern_ = 0;
   volatile int triggerNr_; // total # of triggers in this run (0-based)
   volatile int sequenceNr_; // # of trigger in sequence (0-based)
   int skipTriggers_ = 0;  // # of triggers to skip before starting to generate patterns
   byte currentPattern_ = 0;
   const unsigned long timeOut_ = 1000;
   bool blanking_ = false;
   bool blankOnHigh_ = true;
 
 void setup() {
   // Higher speeds do not appear to be reliable
   Serial.begin(57600);
  
   pinMode(inPin_, INPUT);
   pinMode(dataPin, OUTPUT);
   pinMode(clockPin, OUTPUT);
   pinMode(latchPin, OUTPUT);
   pinMode(LDACPin, OUTPUT);
   pinMode(8, OUTPUT);
   pinMode(9, OUTPUT);
   pinMode(10, OUTPUT);
   pinMode(11, OUTPUT);
   pinMode(12, OUTPUT);
   pinMode(13, OUTPUT);
   pinMode(A0, INPUT);
   
   // Set analogue pins as input:
   DDRC = DDRC & B11000000;
   // Turn on build-in pull-up resistors
   PORTC = PORTC | B00111111;
   
   motor1.setSpeed(50); //sets the Adafruit Motor Shield stepper motor 1 speed in rpm
   motor2.setSpeed(50); //sets the Adafruit Motor Shield stepper motor 2 speed in rpm
   
  /* int origin1 = digitalRead(A0);
   while(origin1 == 0){
     motor1.step(1, BACKWARD, INTERLEAVE);
     origin1 = digitalRead(A0);
     current_position2=0;
   }
   motor1.step(1, BACKWARD, INTERLEAVE);
   
   int origin2 = digitalRead(A1);
   while(origin2 == 0){
     motor2.step(1, BACKWARD, INTERLEAVE);
     origin2 = digitalRead(A1);
     current_position=0;
   }
   motor2.step(1, BACKWARD, INTERLEAVE);
   */ 
 }
 
 void loop() {
   if (Serial.available() > 0) {
     int inByte = Serial.read();
     switch (inByte) {
       
       // Set digital output
     case 1 :
          if (waitForSerial(timeOut_)) {
            currentPattern_ = Serial.read();
            // Do not set bits 6 and 7 (not sure if this is needed..)
            currentPattern_ = currentPattern_ & B00111111;
            
            if (!blanking_){
              if(currentPattern_ == 2)      {mov=200; motor2.step(mov, FORWARD, INTERLEAVE);}          //180            
              else if(currentPattern_ == 4) {mov=100; motor2.step(mov, FORWARD, INTERLEAVE);}          // 90
              else if(currentPattern_ == 6) {mov=50;  motor2.step(mov, FORWARD, INTERLEAVE);}          // 45
              else if(currentPattern_ == 8) {mov=20;  motor2.step(mov, FORWARD, INTERLEAVE);}          // 18
              else if(currentPattern_ == 10) {mov=10; motor2.step(mov, FORWARD, INTERLEAVE);}          //  9
              else if(currentPattern_ == 12) {mov=1; motor2.step(mov, FORWARD, INTERLEAVE);}           //  0.9
              
              else if(currentPattern_ == 3) {mov=200; motor2.step(mov, BACKWARD, INTERLEAVE);}         //180            
              else if(currentPattern_ == 5) {mov=100; motor2.step(mov, BACKWARD, INTERLEAVE);}         // 90
              else if(currentPattern_ == 7) {mov=50;  motor2.step(mov, BACKWARD, INTERLEAVE);}    //B00010110  // 45
              else if(currentPattern_ == 9) {mov=20;  motor2.step(mov, BACKWARD, INTERLEAVE);}    //B00011000  // 18
              else if(currentPattern_ == 11) {mov=10; motor2.step(mov, BACKWARD, INTERLEAVE);}    //B00011010  //  9
              else if(currentPattern_ == 13) {mov=1; motor2.step(mov, BACKWARD, INTERLEAVE);}    //B00011100  //  0.9
              
              else if(currentPattern_ == 14) {x=0;   mov=x-current_position; if(mov<0){mov=mov+400;}; motor2.step(mov, FORWARD, INTERLEAVE); current_position=x;}           
              else if(currentPattern_ == 32) {x=67;  mov=x-current_position; if(mov<0){mov=mov+400;}; motor2.step(mov, FORWARD, INTERLEAVE); current_position=x;} 
              else if(currentPattern_ == 18) {x=133; mov=x-current_position; if(mov<0){mov=mov+400;}; motor2.step(mov, FORWARD, INTERLEAVE); current_position=x;} 
              else if(currentPattern_ == 20) {x=200; mov=x-current_position; if(mov<0){mov=mov+400;}; motor2.step(mov, FORWARD, INTERLEAVE); current_position=x;} 
              else if(currentPattern_ == 22) {x=267; mov=x-current_position; if(mov<0){mov=mov+400;}; motor2.step(mov, FORWARD, INTERLEAVE); current_position=x;} 
              else if(currentPattern_ == 24) {x=333; mov=x-current_position; if(mov<0){mov=mov+400;}; motor2.step(mov, FORWARD, INTERLEAVE); current_position=x;} 

              else if(currentPattern_ == 15) {x=0;   mov=x-current_position2; if(mov<0){mov=mov+400;}; motor1.step(mov, FORWARD, INTERLEAVE); current_position2=x;}            
              else if(currentPattern_ == 17) {x=67;  mov=x-current_position2; if(mov<0){mov=mov+400;}; motor1.step(mov, FORWARD, INTERLEAVE); current_position2=x;} 
              else if(currentPattern_ == 19) {x=133; mov=x-current_position2; if(mov<0){mov=mov+400;}; motor1.step(mov, FORWARD, INTERLEAVE); current_position2=x;} 
              else if(currentPattern_ == 21) {x=200; mov=x-current_position2; if(mov<0){mov=mov+400;}; motor1.step(mov, FORWARD, INTERLEAVE); current_position2=x;} 
              else if(currentPattern_ == 23) {x=267; mov=x-current_position2; if(mov<0){mov=mov+400;}; motor1.step(mov, FORWARD, INTERLEAVE); current_position2=x;} 
              else if(currentPattern_ == 25) {x=333; mov=x-current_position2; if(mov<0){mov=mov+400;}; motor1.step(mov, FORWARD, INTERLEAVE); current_position2=x;} 
              
              /*else if(currentPattern_ == 14) {filterMovement (1, 1);} //B00000010 1st filter          
              else if(currentPattern_ == 32) {filterMovement (1, 2);} //B00000100 2nd filter
              else if(currentPattern_ == 18) {filterMovement (1, 3);} //B00000110 3rd filter
              else if(currentPattern_ == 20) {filterMovement (1, 4);} //B00001000 4th filter
              else if(currentPattern_ == 22) {filterMovement (1, 5);} //B00001010 5th filter
              else if(currentPattern_ == 24) {filterMovement (1, 6);} //B00001100 6th filter           

              else if(currentPattern_ == 15) {filterMovement (2, 1);} //B00110010       
              else if(currentPattern_ == 17) {filterMovement (2, 2);} //B00110100
              else if(currentPattern_ == 19) {filterMovement (2, 3);} //B00110110
              else if(currentPattern_ == 21) {filterMovement (2, 4);} //B00111000
              else if(currentPattern_ == 23) {filterMovement (2, 5);} //B00111010
              else if(currentPattern_ == 25) {filterMovement (2, 6);} //B00111100*/
              
              else {PORTB = currentPattern_;}
            }
            Serial.write( byte(1));
          }
          break;
       // Get digital output
       case 2:
          Serial.write( byte(2));
          Serial.write( PORTB);
          break;
          
       // Set Analogue output (TODO: save for 'Get Analogue output')
       case 3:
         if (waitForSerial(timeOut_)) {
         }
         break;
         
       // Sets the specified digital pattern
       case 5:
          if (waitForSerial(timeOut_)) {
            int patternNumber = Serial.read();
            if ( (patternNumber >= 0) && (patternNumber < SEQUENCELENGTH) ) {
              if (waitForSerial(timeOut_)) {
                triggerPattern_[patternNumber] = Serial.read();
                triggerPattern_[patternNumber] = triggerPattern_[patternNumber] & B00111111;
                Serial.write( byte(5));
                Serial.write( patternNumber);
                Serial.write( triggerPattern_[patternNumber]);
                break;
              }
            }
          }
          Serial.write( "n:");//Serial.print("n:");
          break;
          
       // Sets the number of digital patterns that will be used
       case 6:
         if (waitForSerial(timeOut_)) {
           int pL = Serial.read();
           if ( (pL >= 0) && (pL <= 12) ) {
             patternLength_ = pL;
             Serial.write( byte(6));
             Serial.write( patternLength_);
           }
         }
         break;
         
       // Skip triggers
       case 7:
         if (waitForSerial(timeOut_)) {
           skipTriggers_ = Serial.read();
           Serial.write( byte(7));
           Serial.write( skipTriggers_);
         }
         break;
         
       //  starts trigger mode
       case 8: 
         if (patternLength_ > 0) {
           sequenceNr_ = 0;
           triggerNr_ = -skipTriggers_;
           int state = digitalRead(inPin_);
           PORTB = B00000000;
           Serial.write( byte(8));
           while (Serial.available() == 0) {
             int tmp = digitalRead(inPin_);
             if (tmp != state) {
               if (triggerNr_ >=0) {
                 PORTB = triggerPattern_[sequenceNr_];
                 sequenceNr_++;
                 if (sequenceNr_ >= patternLength_)
                   sequenceNr_ = 0;
               }
               triggerNr_++;
             }
             state = tmp;
           }
           PORTB = B00000000;
         }
         break;
         
         // return result from last triggermode
       case 9:
          Serial.write( byte(9));
          Serial.write( triggerNr_);
          break;
          
       // Sets time interval for timed trigger mode
       // Tricky part is that we are getting an unsigned int as two bytes
       case 10:
          if (waitForSerial(timeOut_)) {
            int patternNumber = Serial.read();
            if ( (patternNumber >= 0) && (patternNumber < SEQUENCELENGTH) ) {
              if (waitForSerial(timeOut_)) {
                unsigned int highByte = 0;
                unsigned int lowByte = 0;
                highByte = Serial.read();
                if (waitForSerial(timeOut_))
                  lowByte = Serial.read();
                highByte = highByte << 8;
                triggerDelay_[patternNumber] = highByte | lowByte;
                Serial.write( byte(10));
                Serial.write(patternNumber);
                break;
              }
            }
          }
          break;

       // Sets the number of times the patterns is repeated in timed trigger mode
       case 11:
         if (waitForSerial(timeOut_)) {
           repeatPattern_ = Serial.read();
           Serial.write( byte(11));
           Serial.write( repeatPattern_);
         }
         break;

       //  starts timed trigger mode
       case 12: 
         if (patternLength_ > 0) {
           PORTB = B00000000;
           Serial.write( byte(12));
           for (byte i = 0; i < repeatPattern_ && (Serial.available() == 0); i++) {
             for (int j = 0; j < patternLength_ && (Serial.available() == 0); j++) {
               PORTB = triggerPattern_[j];
               delay(triggerDelay_[j]);
             }
           }
           PORTB = B00000000;
         }
         break;

       // Blanks output based on TTL input
       case 20:
         blanking_ = true;
         Serial.write( byte(20));
         break;
         
       // Stops blanking mode
       case 21:
         blanking_ = false;
         Serial.write( byte(21));
         break;
         
       // Sets 'polarity' of input TTL for blanking mode
       case 22: 
         if (waitForSerial(timeOut_)) {
           int mode = Serial.read();
           if (mode==0)
             blankOnHigh_= true;
           else
             blankOnHigh_= false;
         }
         Serial.write( byte(22));
         break;
         
       // Gives identification of the device
       case 30:
         Serial.println("MM-Ard");
         break;
         
       // Returns version string
       case 31:
         Serial.println(version_);
         break;

       case 40:
         Serial.write( byte(40));
         Serial.write( PINC);
         break;
         
       case 41:
         if (waitForSerial(timeOut_)) {
           int pin = Serial.read();  
           if (pin >= 0 && pin <=5) {
              int val = analogRead(pin);
              Serial.write( byte(41));
              Serial.write( pin);
              Serial.write( highByte(val));
              Serial.write( lowByte(val));
           }
         }
         break;
         
       case 42:
         if (waitForSerial(timeOut_)) {
           int pin = Serial.read();
           if (waitForSerial(timeOut_)) {
             int state = Serial.read();
             Serial.write( byte(42));
             Serial.write( pin);
             if (state == 0) {
                digitalWrite(14+pin, LOW);
                Serial.write( byte(0));
             }
             if (state == 1) {
                digitalWrite(14+pin, HIGH);
                Serial.write( byte(1));
             }
           }
         }
         break;

       }
    }
    if (blanking_) {
      if (blankOnHigh_) {
        if (digitalRead(inPin_) == LOW)
          PORTB = currentPattern_;
        else
          PORTB = 0;
      } else {
        if (digitalRead(inPin_) == LOW)
          PORTB = 0;
        else     
          PORTB = currentPattern_; 
      }
    }
}

 
bool waitForSerial(unsigned long timeOut)
{
    unsigned long startTime = millis();
    while (Serial.available() == 0 && (millis() - startTime < timeOut) ) {}
    if (Serial.available() > 0)
       return true;
    return false;
 }

// Sets analogue output in the TLV5618
// channel is either 0 ('A') or 1 ('B')
// value should be between 0 and 4095 (12 bit max)
// pins should be connected as described above

void move(int winding) {
  if (winding == 1) {
    digitalWrite(aP1, HIGH);
    digitalWrite(bP2, LOW);
    digitalWrite(aN3, LOW);
    digitalWrite(bN4, LOW);
  }
  if (winding == 2) {
    digitalWrite(aP1, LOW);
    digitalWrite(bP2, HIGH);
    digitalWrite(aN3, LOW);
    digitalWrite(bN4, LOW);
  }
  if (winding == 3) {
    digitalWrite(aP1, LOW);
    digitalWrite(bP2, LOW);
    digitalWrite(aN3, HIGH);
    digitalWrite(bN4, LOW);
  }
  if (winding == 4) {
    digitalWrite(aP1, LOW);
    digitalWrite(bP2, LOW);
    digitalWrite(aN3, LOW);
    digitalWrite(bN4, HIGH);
  }
}

void moveit() {
  int movimento = 1;
  while (steps < stepsFinal) {
  /*
  if (movimento == 1){      //para evitar falhas na comunicação
    Serial.print(1, BYTE);
    movimento = 0;
  }
  */
  delay(delayTime);
    if (previous == 4) {
      if (clockwise == 1) {
        move(1);
        previous = 1;
        steps = steps + 1;
      }
      else {
        move(3);
        previous = 3;
        steps = steps + 1;
      }
    }
    else if (previous == 1) {
      if (clockwise == 1) {
        move(2);
        previous = 2;
        steps = steps + 1;
      }
      else {
        move(4);
        previous = 4;
        steps = steps + 1;
      }
    }
    else if (previous == 2) {
      if (clockwise == 1) {
        move(3);
        previous = 3;
        steps = steps + 1;
      }
      else {
        move(1);
        previous = 1;
        steps = steps + 1;
      }
    }
    else if (previous == 3) {
      if (clockwise == 1) {
        move(4);
        previous = 4;
        steps = steps + 1;
      }
      else {
        move(2);
        previous = 2;
        steps = steps + 1;
      }
    }
  }
}

void filterMovement (int motorNumber, int wantedFilter){
  if (motorNumber == 1){
    int filterChanges = wantedFilter - currentFilter1;
    if (filterChanges < 0){
      filterChanges = filterChanges + 6;
    }
    currentFilter1 = wantedFilter;
    totalfilterChanges1 = totalfilterChanges1 + filterChanges;
    int stepstobedone = filterChanges * 66;
    if(totalfilterChanges1 > 5){
      motor1.step(stepstobedone + 4, BACKWARD, INTERLEAVE);
      totalfilterChanges1 = totalfilterChanges1 - 6;
    }
    else if(totalfilterChanges1 > 3){
      motor1.step(stepstobedone + 2, BACKWARD, INTERLEAVE);
      totalfilterChanges1 = totalfilterChanges1 - 3;
    }
    else {
      motor1.step(stepstobedone, BACKWARD, INTERLEAVE);
    }
  }
  else if (motorNumber == 2){
    int filterChanges = wantedFilter - currentFilter2;
    if (filterChanges < 0){
      filterChanges = filterChanges + 6;
    }
    currentFilter2 = wantedFilter;
    totalfilterChanges2 = totalfilterChanges2 + filterChanges;
    int stepstobedone = filterChanges * 66;
    if(totalfilterChanges2 > 5){
      motor2.step(stepstobedone + 4, BACKWARD, INTERLEAVE);
      totalfilterChanges2 = totalfilterChanges2 - 6;
    }
    else if(totalfilterChanges2 > 3){
      motor2.step(stepstobedone + 2, BACKWARD, INTERLEAVE);
      totalfilterChanges2 = totalfilterChanges2 - 3;
    }
    else {
      motor2.step(stepstobedone, BACKWARD, INTERLEAVE);
    }
  }
}

/* 
 // This function is called through an interrupt   
void triggerMode() 
{
  if (triggerNr_ >=0) {
    PORTB = triggerPattern_[sequenceNr_];
    sequenceNr_++;
    if (sequenceNr_ >= patternLength_)
      sequenceNr_ = 0;
  }
  triggerNr_++;
}


void blankNormal() 
{
    if (DDRD & B00000100) {
      PORTB = currentPattern_;
    } else
      PORTB = 0;
}

void blankInverted()
{
   if (DDRD & B00000100) {
     PORTB = 0;
   } else {     
     PORTB = currentPattern_;  
   }
}   

*/
  


