/*THis is impossible to undertand
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
 
  #define PIN_BITNUM(pin) (PIN ## pin ## _BITNUM)
  #define PIN_PORTREG(pin) (PIN ## pin ## _PORTREG)
  #define PIN_DDRREG(pin) (PIN ## pin ## _DDRREG)
  #define PIN_PINREG(pin) (PIN ## pin ## _PINREG)
  #ifndef _BV
  #define _BV(n) (1<<(n))
  #endif
  
  // send bit b of d
  #define mcpDacSendBit(d, b) {mcpDacSdiSet(d&_BV(b));mcpDacSckPulse();}
  
  //SPI pin definitions
  /** SPI slave select pin. Warning: SS may be redefined as another pin
   but the hardware SS_PIN must be set to output mode before any calls to
   WaveHC functions. The SS_PIN can then be used as a general output pin */
  #define SS   SS_PIN
  /** SPI master output, slave input pin. */
  #define MOSI MOSI_PIN
  /** SPI master input, slave output pin. */
  #define MISO MISO_PIN
  /** SPI serial clock pin. */
  #define SCK  SCK_PIN
  
  //------------------------------------------------------------------------------
  // DAC pin definitions
  // LDAC may be connected to ground to save a pin
  /** Set USE_MCP_DAC_LDAC to 0 if LDAC is grounded. */
  #define USE_MCP_DAC_LDAC 1
  
  // use arduino pins 3, 4, 5, 6 for DAC
  // pin 2 is DAC chip select
  /** Data direction register for DAC chip select. */
  #define MCP_DAC_CS_DDR DDRD
  /** Port register for DAC chip select. */
  #define MCP_DAC_CS_PORT PORTD
  /** Port bit number for DAC chip select. */
  #define MCP_DAC_CS_BIT 5
  
  // pin 3 is DAC serial clock
  /** Data direction register for DAC clock. */
  #define MCP_DAC_SCK_DDR DDRD
  /** Port register for DAC clock. */
  #define MCP_DAC_SCK_PORT PORTD
  /** Port bit number for DAC clock. */
  #define MCP_DAC_SCK_BIT 4
  
  // pin 4 is DAC serial data in
  /** Data direction register for DAC serial in. */
  #define MCP_DAC_SDI_DDR DDRD
  /** Port register for DAC clock. */
  #define MCP_DAC_SDI_PORT PORTD
  /** Port bit number for DAC clock. */
  #define MCP_DAC_SDI_BIT 3
  
  // pin 5 is LDAC if used
  #if USE_MCP_DAC_LDAC
  /** Data direction register for Latch DAC Input. */
  #define MCP_DAC_LDAC_DDR  DDRD
  /** Port register for Latch DAC Input. */
  #define MCP_DAC_LDAC_PORT PORTD
  /** Port bit number for Latch DAC Input. */
  #define MCP_DAC_LDAC_BIT 6
  #endif // USE_MCP_DAC_LDAC
  
  #define mcpDacCsLow() MCP_DAC_CS_PORT &= ~_BV(MCP_DAC_CS_BIT)
  #define mcpDacCsHigh() MCP_DAC_CS_PORT |= _BV(MCP_DAC_CS_BIT)
  #define mcpDacSckLow() MCP_DAC_SCK_PORT &= ~_BV(MCP_DAC_SCK_BIT)
  #define mcpDacSckHigh() MCP_DAC_SCK_PORT |= _BV(MCP_DAC_SCK_BIT)
  #define mcpDacSckPulse() {mcpDacSckHigh();mcpDacSckLow();}
  #define mcpDacSdiLow() MCP_DAC_SDI_PORT &= ~_BV(MCP_DAC_SDI_BIT)
  #define mcpDacSdiHigh() MCP_DAC_SDI_PORT |= _BV(MCP_DAC_SDI_BIT)
  #define mcpDacSdiSet(v) if(v){mcpDacSdiHigh();}else{mcpDacSdiLow();}
  
  //------------------------------------------------------------------------------
  // init dac I/O ports
  inline void mcpDacInit(void) {
    // set all to output mode
    MCP_DAC_CS_DDR |= _BV(MCP_DAC_CS_BIT);
    MCP_DAC_SCK_DDR |= _BV(MCP_DAC_SCK_BIT);
    MCP_DAC_SDI_DDR |= _BV(MCP_DAC_SDI_BIT);
    // chip select high
    mcpDacCsHigh();
  
  #if USE_MCP_DAC_LDAC
    // LDAC low always - use unbuffered mode
    MCP_DAC_LDAC_DDR |= _BV(MCP_DAC_LDAC_BIT);
    MCP_DAC_LDAC_PORT &= ~_BV(MCP_DAC_LDAC_BIT);
  #endif // USE_MCP_DAC_LDAC
  }
  
  //------------------------------------------------------------------------------
  // send 12 bits to dac
  // trusted compiler to optimize and it does 
  // csLow to csHigh takes 8 - 9 usec on a 16 MHz Arduino
  inline void mcpDacSend(uint16_t data) {
    mcpDacCsLow();
    // send DAC config bits
    mcpDacSdiLow();
    mcpDacSckPulse();  // DAC A
    mcpDacSckPulse();  // unbuffered
    mcpDacSdiHigh();
    mcpDacSckPulse();  // 1X gain
    mcpDacSckPulse();  // no SHDN
    // send 12 data bits
    mcpDacSendBit(data, 11);
    mcpDacSendBit(data, 10);
    mcpDacSendBit(data,  9);
    mcpDacSendBit(data,  8);
    mcpDacSendBit(data,  7);
    mcpDacSendBit(data,  6);
    mcpDacSendBit(data,  5);
    mcpDacSendBit(data,  4);
    mcpDacSendBit(data,  3);
    mcpDacSendBit(data,  2);
    mcpDacSendBit(data,  1);
    mcpDacSendBit(data,  0);
    mcpDacCsHigh();
  }
  
  int fa = 200;
  word I[200];
  int j = 0;
  float deltaT;
 
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
 
 
 ////////////////// FILTER WHEEL CONTROL  /////
  // #include <AFMotor.h> //to include the Adafruit Motor Shield library
  //AF_Stepper motor1(200, 1); //defines class motor as 200 steps motor working on M1 and M2
  //AF_Stepper motor2(200, 2); //defines class motor as 200 steps motor working on M3 and M4
 
 void setup() {
   // Higher speeds do not appear to be reliable
   Serial.begin(57600);
  
   pinMode(inPin_, INPUT);
   pinMode (dataPin, OUTPUT);
   pinMode (clockPin, OUTPUT);
   pinMode (latchPin, OUTPUT);
   pinMode (LDACPin, OUTPUT);
   pinMode(8, OUTPUT);
   pinMode(9, OUTPUT);
   pinMode(10, OUTPUT);
   pinMode(11, OUTPUT);
   pinMode(12, OUTPUT);
   pinMode(13, OUTPUT);
   
   // Set analogue pins as input:
   DDRC = DDRC & B11000000;
   // Turn on build-in pull-up resistors
   PORTC = PORTC | B00111111;
   
   I[0] = 0;
   mcpDacInit();
   ////////////////// FILTER WHEEL CONTROL  /////
   //motor1.setSpeed(50); //sets the Adafruit Motor Shield stepper motor 1 speed in rpm
   //motor2.setSpeed(50); //sets the Adafruit Motor Shield stepper motor 2 speed in rpm
    
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
              if(currentPattern_ == 2) {steps = 0; stepsFinal = 100; clockwise=0; moveit();}        //B00000010  //180            
              else if(currentPattern_ == 4) {steps = 0; stepsFinal = 50; clockwise=0; moveit();}     //B00000100  // 90
              else if(currentPattern_ == 6) {steps = 0; stepsFinal = 25; clockwise=0; moveit();}    //B00000110  // 45
              else if(currentPattern_ == 8) {steps = 0; stepsFinal = 10; clockwise=0; moveit();}     //B00001000  // 18
              else if(currentPattern_ == 10) {steps = 0; stepsFinal = 5; clockwise=0; moveit();}     //B00001010  //  9
              else if(currentPattern_ == 12) {steps = 0; stepsFinal = 1; clockwise=0; moveit();}     //B00001100  //  1.8
              else if(currentPattern_ == 14) {steps = 0; stepsFinal = 3; clockwise=0; moveit();}     //B00001100  //  1.8
              
              else if(currentPattern_ == 3) {steps = 0; stepsFinal = 100; clockwise=1; moveit();}   //B00010010  //180            
              else if(currentPattern_ == 5) {steps = 0; stepsFinal = 50; clockwise=1; moveit();}    //B00010100  // 90
              else if(currentPattern_ == 7) {steps = 0; stepsFinal = 25; clockwise=1; moveit();}    //B00010110  // 45
              else if(currentPattern_ == 9) {steps = 0; stepsFinal = 10; clockwise=1; moveit();}     //B00011000  // 18
              else if(currentPattern_ == 11) {steps = 0; stepsFinal = 5; clockwise=1; moveit();}     //B00011010  //  9
              else if(currentPattern_ == 13) {steps = 0; stepsFinal = 1; clockwise=1; moveit();}     //B00011100  //  1.8
              else if(currentPattern_ == 15) {steps = 0; stepsFinal = 3; clockwise=1; moveit();}     //B00011100  //  1.8
            
          /*  else if(currentPattern_ == 14) {motor1.step(67, BACKWARD, INTERLEAVE);}  //B00000010           
              else if(currentPattern_ == 32) {motor1.step(134, BACKWARD, INTERLEAVE);}  //B00000100
              else if(currentPattern_ == 18) {motor1.step(200, BACKWARD, INTERLEAVE);} //B00000110
              else if(currentPattern_ == 20) {motor1.step(266, BACKWARD, INTERLEAVE);} //B00001000
              else if(currentPattern_ == 22) {motor1.step(334, BACKWARD, INTERLEAVE);} //B00001010
              else if(currentPattern_ == 24) {motor1.step(400, BACKWARD, INTERLEAVE);} //B00001100

              else if(currentPattern_ == 15) {motor2.step(67, BACKWARD, INTERLEAVE);}  //B00110010       
              else if(currentPattern_ == 17) {motor2.step(134, BACKWARD, INTERLEAVE);}  //B00110100
              else if(currentPattern_ == 19) {motor2.step(200, BACKWARD, INTERLEAVE);} //B00110110
              else if(currentPattern_ == 21) {motor2.step(266, BACKWARD, INTERLEAVE);} //B00111000
              else if(currentPattern_ == 23) {motor2.step(334, BACKWARD, INTERLEAVE);} //B00111010
              else if(currentPattern_ == 25) {motor2.step(400, BACKWARD, INTERLEAVE);} //B00111100
              */
              //else if(currentPattern_ == 32) {PORTB=16;}
              
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
           int channel = Serial.read();
           if (waitForSerial(timeOut_)) {
              byte msbA = Serial.read();
              msbA &= B00001111;
              if (waitForSerial(timeOut_)) {
                byte lsbA = Serial.read();
                if (waitForSerial(timeOut_)) {
                  byte msbF = Serial.read();
                  msbF &= B00001111;
                  if (waitForSerial(timeOut_)) {
                    byte lsbF = Serial.read();

                    Serial.write( byte(3));
                    Serial.write( channel);
                    Serial.write(msbA);
                    Serial.write(lsbA);
                    Serial.write(msbF);
                    Serial.write(lsbF);
                    word A = word(msbA,lsbA);
                    word F = word(msbF,lsbF);
                    float offset=A/2;
                    byte msbAmax = B00001111;
                    byte lsbAmax = B11111111;
                    //float Amax=3;
                    word Amax= word(msbAmax,lsbAmax);
                    
                    I[0] = Amax/2-offset;
                    int deltaT=1;
                    
                    if (lsbF!=0){
                        float deltaA = A/(fa/2);
                        deltaT =(1/(float(fa)*F))*1000000;                  

                       for (int i = 1; i <= fa/2; i++) {
                          I[i] = word(I[i-1] + deltaA);
                        }
                        for (int i = fa/2 + 1; i < fa; i++) {
                          I[i] = word(I[i-1] - deltaA);
                        }  
                        
                        
                       analogueOut(channel, deltaT);                     
                     }
                    else{
                       mcpDacSend(A+Amax/2);                     
                     }
                  }
                }
              }
           }
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
void analogueOut(int channel, int deltaT) 
{
  int state;
  //for(long int n=0;n<100000;n++){
 while(!state){
    if (j == fa) j = 0;
    byte msb = highByte(I[j]);
    byte lsb = lowByte(I[j]);
    //msb |= B01110000;
    word Afinale = word(msb,lsb);
    mcpDacSend(Afinale);
    if (deltaT > 0) delayMicroseconds(deltaT);
    j++;    
    state = digitalRead(inPin_);
  }
}

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
  


