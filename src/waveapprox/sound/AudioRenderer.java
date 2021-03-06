package waveapprox.sound;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Collections;

import waveapprox.OSUtil;
import de.sciss.net.OSCBundle;

public class AudioRenderer {
	public static final String headerFormat = "WAV";
	public static final String sampleFormat = "int16";
	
	private double sampleRate;
	private int channels;
	private File oscFile;
	private File outputFile;
	private BundleManager bundleManager;
	
	public AudioRenderer(double sampleRate, int channels) {
		this.sampleRate = sampleRate;
		this.channels = channels;
		long millis = System.currentTimeMillis();
		oscFile = new File(OSUtil.getWorkingDirectory() + "/" + millis + ".osc");
		outputFile = new File(OSUtil.getWorkingDirectory() + "/" + millis + ".wav");
		bundleManager = new BundleManager();
	}
	
	public File getOscFile() {
		return oscFile;
	}
	
	public File getOutputFile() {
		return outputFile;
	}
	
	public BundleManager getBundleManager() {
		return bundleManager;
	}
	
	public void render() throws IOException {
		// Sort bundles
		Collections.sort(bundleManager.getBundles(), new BundleManager.OSCBundleComparator());
		
		// Add up the size of all messages
		int bufferSize = 0;
		for(OSCBundle b : bundleManager.getBundles()) {
			bufferSize += 4 + b.getSize();
		}
		
		// Allocate a buffer and encode messages
		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		for(OSCBundle b : bundleManager.getBundles()) {
			buffer.putInt(b.getSize());
			b.encode(buffer);
		}
		
		// Create the output file if it does not exist
		if(!oscFile.exists()) {
		    oscFile.createNewFile();
		}
		
		// Create a print stream
		PrintStream outputStream = new PrintStream(oscFile);
		
		// Output buffer to print stream
		//OSCBundle.printHexOn(outputStream, buffer);
		outputStream.write(buffer.array());
		
		// Close the stream
		outputStream.close();
		
		// NRT scsynth
		Process process;
		if(OSUtil.isWindows()) {
			process = Runtime.getRuntime().exec("");
		} else if(OSUtil.isMac() || OSUtil.isUnix()) {
			String command =
					OSUtil.getSCProgram() + " -N " +
					oscFile + " _ " +
					outputFile + " " +
					sampleRate + " " +
					headerFormat + " " +
					sampleFormat + " -o " +
					channels;
			process = Runtime.getRuntime().exec(command);
		} else {
			process = null;
			System.out.println("operating system not supported");
			System.exit(0);
		}
		
		// Wait for process to finish
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Delete OSC file
		oscFile.delete();
	}
}
