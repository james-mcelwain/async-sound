async function main() {
	const midi = await navigator.requestMIDIAccess()
	const hw1 = Array.from(midi.inputs.values()).find(x => x.name === "VirMIDI 1-0")
	hw1.onmidimessage = console.log.bind(console)
}
