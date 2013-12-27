/**
 * Touch Pixel from Seeed Studio
 *
 * require libraries - PN532, NDEF and ChainableLED
 */

#include <ChainableLED.h>

#include "PN532_HSU.h"
#include "emulatetag.h"
#include "NdefMessage.h"

PN532_HSU interface(Serial1);
EmulateTag nfc(interface);

uint8_t ndefBuf[120];
NdefMessage message;
int messageSize;

uint8_t uid[3] = { 0x12, 0x34, 0x56 };

ChainableLED rgbled(3, 2, 2);

void setup()
{
    Serial.begin(115200);
    Serial.println("------- Emulate Tag --------");
    
    rgbled.setColorRGB(0, 0, 0x00, 0);
    rgbled.setColorRGB(1, 0, 0x00, 0);

    NdefRecord aarRecord = NdefRecord();
    const uint8_t aarType[] = "android.com:pkg";
    const uint8_t aarPayload[] = "com.seeedstudio.android.nfc.touchpixel";

    aarRecord.setTnf(TNF_EXTERNAL_TYPE);
    aarRecord.setType(aarType, sizeof(aarType) - 1);
    aarRecord.setPayload(aarPayload, sizeof(aarPayload) - 1);

    message = NdefMessage();
    message.addMimeMediaRecord("text/c", "FF00FF00");
    message.addRecord(aarRecord);
    messageSize = message.getEncodedSize();
    if (messageSize > sizeof(ndefBuf)) {
        Serial.println("ndefBuf is too small");
        while (1) { }
    }

    Serial.print("Ndef encoded message size: ");
    Serial.println(messageSize);

    message.encode(ndefBuf);

    // comment out this command for no ndef message
    nfc.setNdefFile(ndefBuf, messageSize);

    // uid must be 3 bytes!
    nfc.setUid(uid);

    nfc.init();
    nfc.attach(processNewNdef);
}

void processNewNdef(uint8_t *tag_buf, uint16_t length)
{
    if (nfc.writeOccured()) {
        Serial.println("\nWrite occured !");
        
        nfc.getContent(&tag_buf, &length);
        NdefMessage msg = NdefMessage(tag_buf, length);
        msg.print();

        NdefRecord record = msg.getRecord(0);
        uint8_t recordbuf[32];
        record.getType(recordbuf);
        if (!memcmp(recordbuf, "text/c", 6)) {
            record.getPayload(recordbuf);
            uint32_t color = getColor(recordbuf);
            uint8_t r = (color >> 16) & 0xFF;
            uint8_t g = (color >> 8) & 0xFF;
            uint8_t b = color & 0xFF;
            Serial.println("Color:");
            Serial.println(r);
            Serial.println(g);
            Serial.println(b);
            
            rgbled.setColorRGB(0, r, g, b);
            rgbled.setColorRGB(1, r, g, b);
        }
    }
}

void loop()
{
    // uncomment for overriding ndef in case a write to this tag occured
    //nfc.setNdefFile(ndefBuf, messageSize);

    // start emulation (blocks)
    nfc.emulate();

    // or start emulation with timeout
    /*if(!nfc.emulate(1000)){ // timeout 1 second
      Serial.println("timed out");
    }*/

    // deny writing to the tag
    // nfc.setTagWriteable(false);
    delay(1000);
}

uint32_t getColor(uint8_t *buf)
{
    uint32_t x = 0;
    for (uint8_t i = 0; i < 8; i++) {
        uint8_t c = *buf;
        if (c >= '0' && c <= '9') {
            x *= 16;
            x += c - '0';
        } else if (c >= 'A' && c <= 'F') {
            x *= 16;
            x += (c - 'A') + 10;
        } else if (c >= 'a' && c <= 'f') {
            x *= 16;
            x += (c - 'a') + 10;
        } else 
            break;

        buf++;
    }

    return x;
}
