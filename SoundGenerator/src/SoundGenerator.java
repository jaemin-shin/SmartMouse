import java.applet.*;
import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

public final class SoundGenerator {

    public static final int SAMPLE_RATE = 44100;
    private static final int BYTES_PER_SAMPLE = 2;                // 16-bit audio
    private static final int BITS_PER_SAMPLE = 16;                // 16-bit audio
    private static final double MAX_16_BIT = Short.MAX_VALUE;     // 32,767
    private static final int SAMPLE_BUFFER_SIZE = 4096;
    private static final int amplitude = 1;


    private static SourceDataLine line;   // to play the sound
    private static byte[] buffer;         // our internal buffer
    private static int bufferSize = 0;
    private static AudioFormat format;
    private static AudioInputStream ais;
    private static ByteArrayInputStream bais;
    private static BufferedInputStream bis;
    private static File file;
    private static AudioFileFormat.Type type = AudioFileFormat.Type.WAVE;
    private static byte[] buffer_total;
    private static int count = 0;

    // not-instantiable
    private SoundGenerator() {
    }


    // static initializer
    static {
        init();
    }

    // open up an audio stream
    private static void init() {
        try {
            // 44,100 samples per second, 16-bit audio, mono, signed PCM, little Endian
            format = new AudioFormat((float) SAMPLE_RATE, BITS_PER_SAMPLE, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, SAMPLE_BUFFER_SIZE * BYTES_PER_SAMPLE);
            buffer = new byte[SAMPLE_BUFFER_SIZE * BYTES_PER_SAMPLE / 3];
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        // no sound gets made before this call

        line.start();
    }


    /**
     * Close standard audio.
     */
    public static void close() {
        line.drain();
        line.stop();
    }

    /**
     * Write one sample (between -1.0 and +1.0) to standard audio. If the sample
     * is outside the range, it will be clipped.
     */
    public static void play(double in) {

        // clip if outside [-1, +1]
        if (in < -1.0) in = -1.0;
        if (in > +1.0) in = +1.0;

        // convert to bytes
        short s = (short) (MAX_16_BIT * in);
        buffer[bufferSize++] = (byte) s;
        buffer[bufferSize++] = (byte) (s >> 8);   // little Endian

        // send to sound card if buffer is full
        if (bufferSize >= buffer.length) {
            line.write(buffer, 0, buffer.length);

            count ++;
            System.arraycopy(buffer, 0, buffer_total, buffer.length * (count - 1), buffer.length);

            bufferSize = 0;
        }
    }

    /**
     * Write an array of samples (between -1.0 and +1.0) to standard audio. If a sample
     * is outside the range, it will be clipped.
     */
    public static void play(double[] input) {
        buffer_total = new byte[input.length*2];
        for (int i = 0; i < input.length; i++) {
            play(input[i]);
        }
        file = new File("test19000.wav");
        bais = new ByteArrayInputStream(buffer_total);
        bis  = new BufferedInputStream(bais);
        ais = new AudioInputStream(bis, format, (long) buffer_total.length);
    }

    private static double[] tone(double hz, double duration) {
        int N = (int) (SoundGenerator.SAMPLE_RATE * duration);
        double[] a = new double[N + 1];
        for (int i = 0; i <= N; i++)
            a[i] = amplitude * Math.sin(2 * Math.PI * i * hz / SoundGenerator.SAMPLE_RATE);
        return a;
    }


    /**
     * Test client - play an A major scale to standard audio.
     */
    public static void main(String[] args) {


        double hz = 19000;// 440.4 Hz for 1 sec
        double duration = 100.0;

        double[] a = tone(hz, duration);
        SoundGenerator.play(a);
        try {
            AudioSystem.write(ais, type, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}