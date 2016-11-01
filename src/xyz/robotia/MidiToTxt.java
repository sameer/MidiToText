package xyz.robotia;

import javax.sound.midi.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IllegalFormatException;

/**
 * Created by Sameer Puri (Robotia) on 11/1/16.
 */
public class MidiToTxt {
    public static void main(String[] args) {
        try {
            new MidiToTxt().run(args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private void run(String[] args) throws Throwable {
        if (args.length != 2) {
            System.out.println("Usage: java -jar MidiToTxt.jar infile.mid outfile.txt");
            System.exit(0);
        }
        Sequence seq = MidiSystem.getSequence(new File(args[0]));
        Track toProcess = null;
        for (Track track : seq.getTracks()) {
            if (toProcess == null) {
                toProcess = track;
            } else if (track.size() > toProcess.size()) {
                toProcess = track;
            }
        }
        if (toProcess != null) {
            byte[] time_signature = null;
            ArrayList<long[]> songData = new ArrayList<>(toProcess.size());
            for (int i = 0; i < toProcess.size(); i++) {
                MidiEvent midiEvent = toProcess.get(i);
                MidiMessage midiMessage = midiEvent.getMessage();
                if (midiMessage instanceof MetaMessage) {
                    MetaMessage metaMessage = (MetaMessage) midiMessage;
                    if (metaMessage.getType() == 0x58) {
                        time_signature = metaMessage.getData();
                    } else if (metaMessage.getType() == 0x2f) { // Meta Message for end of song
                        break;
                    }
                } else if (midiMessage instanceof ShortMessage) {
                    ShortMessage shortMessage = (ShortMessage) midiMessage;
                    if (shortMessage.getCommand() == ShortMessage.NOTE_ON) {
                        songData.add(new long[]{midiEvent.getTick(), shortMessage.getData1()});
                    }
                }
            }
            if (time_signature == null) {
                System.out.println("No time signature meta message was found! Assuming defaults!");
                time_signature = new byte[]{4, 4, 24, 8};
            }
            PrintWriter out = new PrintWriter(new File(args[1]));
            double ppq = seq.getResolution();
            long last = 0;
            for (long[] note : songData) {
                if (note[1] < 45 || note[1] > 81) { // We can't process notes below A2 (110Hz) and above A5 (880Hz)
                    continue;
                }
                out.println((note[0] / ppq) + " " + (note[1] - 45));
                if (note[0] > last) last = note[0];
            }
            out.println((last / ppq) + " -1");
            out.close();
            System.out.println("Successfully transcoded " + args[0] + " to " + args[1]);
        } else {
            throw new Exception("Invalid MIDI file! No tracks detected!");
        }
    }
}
