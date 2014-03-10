/*
 * Title:        Bluetooth Remote Control
 * Description:  This program is intented to work with a connected
 *               Bluetooth dongle to the robot and an Android phone
 *               running ALFRED application
 * Author:       Jon Dyson <jon@thedysongang.com>
 * Version:      1.0
 * Website:      github
 */
#define LED PC(2)

char serialData[32];
byte com = 0;
byte error = 0;
byte timerCounter = 0;
boolean connected;

void setup()
{
  pinMode(LED, OUTPUT);
  // the bluetooth dongle communicates at 9600 baud only
  Serial1.begin(115200);
  // set up timeout timer on timer2 in case of lost connection
  // initialize counter
  TCNT2 = 0;
  // set prescaler to 256
  TCCR2 = 0x06;
  // enable overflow interrupt
  TIMSK |= (1 << TOIE2);  // << = bitwise shift left
  // enable global interrupts
  asm("SEI");
}

void loop()
{
  if(Serial1.available() > 0)
  {
    digitalWrite(LED, HIGH);
    connected = true;
    // clear timeout
    com = timerCounter;

    Serial1.readBytesUntil('\n', serialData, 31);
    switch(serialData[0])
    {
    case 0:
      Serial1.println(0);
      break;
    case 'a':
      // use as a small and slow oscilloscope
      int pin;
      if(parseCommand(serialData, &pin, 1) && pin >= 0 && pin <= 7)
      {
        // stop loop by sending something to the robot
        while(!Serial1.available() && connected)
        {
          Serial1.println(analogRead(pin));
        }
      }
      else
      {
        Serial1.println("Error while setting ADC pin");
      }
      break;
    case 's':
      // set left and right motor speeds
      int speed[2];
      if(parseCommand(serialData, speed, 2))
      {
        setSpeed(speed[0], speed[1]);
        Serial1.println("New speed set");
      }
      else
      {
        Serial1.println("Error while setting new speed");
      }
      break;
    case 'i':
      // inform about robot
      Serial1.println("ALFRED 1.0");
      break;
    case 'r':
      // quickly stop
      reset();
      Serial1.println("Robot reset");
      break;
    default:
      // inform user of non existing command
      Serial1.println("Command not recognised");
    }

    // clear serialData array
    memset(serialData, 0, sizeof(serialData));
  }
}

void reset()
{
  connected = false;
  setSpeed(0, 0);
}

/**
 * This function makes ints out of the received serial data, the 2 first
 * characters are not counted as they consist of the command character and
 * a comma separating the first variable.
 *
 * @params command The whole serial data received as an address
 * @params returnValues The array where the return values go as an address
 * @params returnNumber The number of values to set inside the returnValues variable
 */
boolean parseCommand(char* command, int* returnValues, byte returnNumber)
{
  // parsing state machine
  byte i = 1, j = 0, sign = 0, ch = 0, number;
  int temp = 0;
  while(i++)
  {
    switch(*(command + i))
    {
    case '\0':
    case ',':
      // set return value
      if(ch != 0)
      {
        returnValues[j++] = sign?-temp:temp;
        sign = 0;
        temp = 0;
        ch = 0;
      }
      else
      {
        return false;
      }
      break;
    case '-':
      sign = 1;
      break;
    default:
      // convert string to int
      number = *(command + i) - '0';
      if(number < 0 || number > 9)
      {
        return false;
      }
      temp = temp * 10 + number;
      ch++;
    }

    // enough return values have been set
    if(j == returnNumber)
    {
      return true;
    }
    // end of command reached
    else if(*(command + i) == '\0')
    {
      return false;
    }
  }
}

ISR(TIMER2_OVF_vect)
{
  // 8e7Hz / 256 / 256 / 1220 = 1Hz
  if(timerCounter++ < 1220)
  {
    return;
  }

  // verify communication status
  if(com == 0)
  {
    // no data has been passed since last time
    // interpret as communication failure
    digitalWrite(LED, LOW);
    reset();
  }
  com = 0;
  timerCounter = 0;
}

// set motor speed for both wheels, uses timer1 THIS NEEDS CHANGING
void setSpeed(int left, int right)
{
  // PD4-7 as output
  DDRD |= 0xf0;
  // set Waveform Generation Mode
  TCCR1A = 0x03; // (1<<WGM11)|(1<<WGM10): PWM, phase correct, 10bits
  TCCR1B = 0x01; // (1<<CS10): set prescaling of 1 (8MHz/1 = 8MHz)

  if(left == 0)
  {
    // normal port operation, OC1A disconnected
    TCCR1A &= ~(3 << COM1A0);
    PORTD &= ~(1 << PD5);
  }
  else
  {
    // clear OC1A on Compare Match (set output to low level)
    TCCR1A |= (1 << COM1A1);

    // set left motor direction (PD7)
    if(left < 0)
    {
      left = -left;
      PORTD |= (1 << 7);
    }
    else
    {
      PORTD &= ~(1 << 7);
    }

    // limit to maximum speed
    if(left > 100)
    {
      left = 1023;
    }
    else
    {
      left = (long) 1023*left/100;	
    }
  }

  if(right == 0)
  {
    // normal port operation, OC1B disconnected
    TCCR1A &= ~(3 << COM1B0);
    PORTD &= ~(1 << PD4);
  }
  else
  {
    // clear OC1B on Compare Match (set output to low level)
    TCCR1A |= (1 << COM1B1);

    // set right motor direction (PD6)
    if(right < 0)
    {
      right = -right;
      PORTD |= (1 << 6);
    }
    else
    {
      PORTD &= ~(1 << 6);
    }

    // limit to maximum speed
    if(right > 100)
    {
      right = 1023;
    }
    else
    {
      right = (long) 1023*right/100;
    }
  }

  // first write high byte (important, see datasheet)
  OCR1AH = left >> 8;
  OCR1AL = left & 0xff;
  OCR1BH = right >> 8;
  OCR1BL = right & 0xff;
}



