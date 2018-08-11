extern crate midir;
extern crate redis;

use redis::{Client, Commands};
use midir::MidiInput;

fn main() {
    let midi_in = MidiInput::new("bridge").unwrap();

    if midi_in.port_count() < 2 {
        println!("No ports found");
    } else {
        println!("{} ports found", midi_in.port_count());
    }

    let port_number = 1;

    let midi_info = midi_in.port_name(port_number).unwrap();

    println!("Using virtual device {:?}", midi_info);

    let client = get_client();

    println!("Connecting...");
    let _con = midi_in.connect(port_number, "", move |_time, ba, _| {
        if ba.len() == 3 {
            let cc = ba[1];
            let val = ba[2];

            match client.get_connection().unwrap().set::<u8, u8, ()>(cc, val) {
                Ok(_) => println!("{}:{}", cc, val),
                Err(e) => println!("Dropped message {}", e),
            };
        }
    }, ()).unwrap();
    println!("Connected.");
    loop {}
}

fn get_client() -> Client {
    Client::open("redis://127.0.0.1/").unwrap()
}
