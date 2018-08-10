extern crate midir;
extern crate redis;

use redis::{Client, Commands};
use midir::MidiInput;

fn main() {
    let midi_in = MidiInput::new("bridge").unwrap();

    if midi_in.port_count() < 2 {
        println!("No ports found");
    }

    let port_number = 2;

    let midi_info = midi_in.port_name(port_number).unwrap();

    println!("Using virtual device {:?}", midi_info);

    let client = get_client();

    println!("Connecting...");
    midi_in.connect(port_number, "", move |_time, ba, _| {
        println!("Message {:?}", ba);

        let cc = ba[1];
        let val = ba[2];

        match client.get_connection().unwrap().set::<u8, u8, u8>(cc, val) {
            Ok(_) => println!("{}:{}", cc, val),
            Err(e) => println!("Dropped message {}", e),
        };
    }, ()).unwrap();
    println!("Connected.");
    loop {}
}

fn get_client() -> Client {
    Client::open("redis://127.0.0.1/").unwrap()
}
