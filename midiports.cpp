#include <iostream>
#include <cstdlib>
#include "RtMidi.h"
void mycallback(double deltatime, std::vector< unsigned char > *message, void *userData) {
  unsigned int nBytes = message->size();
  for ( unsigned int i=0; i<nBytes; i++ )
    std::cout << "Byte " << i << " = " << (int)message->at(i) << ", ";
  if ( nBytes > 0 )
    std::cout << "stamp = " << deltatime << std::endl;
}

int main() {
  RtMidiIn *midiin = new RtMidiIn();

  unsigned int nPorts = midiin->getPortCount();

  std::cout << nPorts;
  if (nPorts == 0) {
    std::cout << "No ports available!\n";
    goto cleanup;
  }
  midiin->openPort( 1 );

  midiin->setCallback( &mycallback );

  std::cout << "\nReading MIDI input ... press <enter> to quit.\n";
  char input;
  std::cin.get(input);

 cleanup:
  delete midiin;
  return 0;
}

