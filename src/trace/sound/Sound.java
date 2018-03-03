package trace.sound;

import field.linalg.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;
import org.lwjgl.util.WaveData;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;


public class Sound {

	private long context;

	public class Buffer {
		IntBuffer buffer = BufferUtils.createIntBuffer(1);
	}

	public class Source {
		IntBuffer source = BufferUtils.createIntBuffer(1);
		FloatBuffer sourcePos = BufferUtils.createFloatBuffer(3).put(
			new float[]{0.0f, 0.0f, 0.0f});
		FloatBuffer sourceVel = BufferUtils.createFloatBuffer(3).put(
			new float[]{0.0f, 0.0f, 0.0f});
		IntBuffer offset = BufferUtils.createIntBuffer(1).put(
			new int[]{0,});
	}

	List<Source> sources = new ArrayList<>();
	List<Source> free = new ArrayList<>();

	public Source makeSource() {
		Source s = new Source();
		AL10.alGenSources(s.source);
		if (AL10.alGetError() != AL10.AL_NO_ERROR)
			return null;

		AL10.alSourcef(s.source.get(0), AL10.AL_PITCH, 1.0f);
		AL10.alSourcef(s.source.get(0), AL10.AL_GAIN, 1.0f);
		s.sourcePos.rewind();
		s.sourceVel.rewind();
		AL10.alSource3f(s.source.get(0), AL10.AL_POSITION, 0, 0, 0);
		AL10.alSource3f(s.source.get(0), AL10.AL_VELOCITY, 0, 0, 0);
		s.sourcePos.rewind();
		s.sourceVel.rewind();

		sources.add(s);
		free.add(s);
		return s;
	}

	public Sound init(int num) {

		long device = alcOpenDevice((ByteBuffer) null);

		ALCCapabilities deviceCaps = ALC.createCapabilities(device);

		context = alcCreateContext(device, (IntBuffer) null);
		alcMakeContextCurrent(context);
		AL.createCapabilities(deviceCaps);

		System.out.println("devices :" + ALC10.alcGetString(0, ALC10.ALC_EXTENSIONS));
		System.out.println("devices :" + ALC10.alcGetString(context, ALC10.ALC_DEVICE_SPECIFIER));
		System.out.println("devices :" + AL10.alGetString(AL10.AL_VENDOR));
		System.out.println("devices :" + AL10.alGetString(AL10.AL_RENDERER));
		System.out.println("devices :" + AL10.alGetString(AL10.AL_VERSION));
		System.out.println("devices :" + ALC10.alcGetInteger(context, ALC10.ALC_MAJOR_VERSION));
		System.out.println("devices :" + ALC10.alcGetInteger(context, ALC10.ALC_MINOR_VERSION));
		for (int i = 0; i < num; i++)
			makeSource();
		System.out.println(" init openal :" + AL10.alGetError() + " "
			+ sources.size() + " sources");

		return this;
	}

	public Source allocate() {
		if (free.size() == 0)
			return null;
		return free.remove(0);
	}

	public Supplier<Double> play(Buffer f, Source s, float pitch, float gain,
				     Vec3 position, Vec3 velocity) {
		return
			play(f, s, pitch, gain, position, velocity, 0);
	}

	public Supplier<Double> play(Buffer f, Source s, float pitch, float gain,
				     Vec3 position, Vec3 velocity, int positionInSames) {
		AL10.alSourcei(s.source.get(0), AL10.AL_BUFFER, f.buffer.get(0));

		//AL10.alSourcef(s.source.get(0), AL10.AL_PITCH, 1.0f);
		AL10.alSourcef(s.source.get(0), AL10.AL_GAIN, gain);

		s.sourcePos.rewind();
		s.sourcePos.put((float) position.x);
		s.sourcePos.put((float) position.y);
		s.sourcePos.put((float) position.z);
		s.sourcePos.rewind();

		s.sourceVel.rewind();
		s.sourceVel.put((float) velocity.x);
		s.sourceVel.put((float) velocity.y);
		s.sourceVel.put((float) velocity.z);
		s.sourceVel.rewind();

		s.offset.rewind();
		s.offset.put(positionInSames);
		s.offset.rewind();

		AL10.alSource3f(s.source.get(0), AL10.AL_POSITION, (float) position.x, (float) position.y, (float) position.z);
		AL10.alSource3f(s.source.get(0), AL10.AL_VELOCITY, (float) velocity.x, (float) velocity.y, (float) velocity.z);
		AL10.alSourcei(s.source.get(0), AL11.AL_SAMPLE_OFFSET, s.offset.get());
		AL10.alSourcePlay(s.source.get(0));

		return () -> {
			float v = AL10.alGetSourcef(s.source.get(0), AL11.AL_SEC_OFFSET);
			return (double) v;
		};
	}

	public void stop(Source s) {
		AL10.alSourceStop(s.source.get(0));
	}

	public void pause(Source s) {
		AL10.alSourcePause(s.source.get(0));
	}

	public void play(Source s) {
		AL10.alSourcePlay(s.source.get(0));
	}


	public void deallocate(Source s) {
		AL10.alSourceStop(s.source.get(0));
		AL10.alSourcei(s.source.get(0), AL10.AL_BUFFER, 0);
		free.add(s);
	}

	static public FloatBuffer floatsFromFile(String filename)
		throws FileNotFoundException {

		WaveData data = WaveData.create(new BufferedInputStream(
			new FileInputStream(filename)));

		System.out.println(" format :" + data.format + " " + data.samplerate
			+ " " + data.data);
		try {
			switch (data.format) {
				case AL10.AL_FORMAT_MONO16: {
					FloatBuffer f = FloatBuffer.allocate(data.data.capacity() / 2);
					System.out.println(" mono 16");
					ShortBuffer s = data.data.asShortBuffer();
					for (int i = 0; i < s.capacity(); i++) {
						f.put(s.get(i) / (float) Short.MAX_VALUE);
					}
					return f;
				}
				case AL10.AL_FORMAT_STEREO16: {
					FloatBuffer f = FloatBuffer.allocate(data.data.capacity() / 4);
					System.out.println(" stereo 16, taking left channel");
					ShortBuffer s = data.data.asShortBuffer();
					for (int i = 0; i < s.capacity() / 2; i++) {
						f.put(s.get(i * 2) / (float) Short.MAX_VALUE);
					}
					return f;
				}
				default:
					break;
			}
		} finally {
			data.dispose();
		}
		return null;
	}

	public Buffer makeBufferFromFile(String filename)
		throws FileNotFoundException {
		WaveData data = WaveData.create(new BufferedInputStream(
			new FileInputStream(filename)));

		System.out.println(" loaded :" + data.format + " " + data.samplerate
			+ " " + data.data);
		Buffer r = new Buffer();
		AL10.alGenBuffers(r.buffer);
		AL10.alBufferData(r.buffer.get(0), data.format, data.data,
			data.samplerate);
//		AL10.alBufferData(r.buffer.get(0), 0x1205, data.data,
//				data.samplerate);
		data.dispose();

		return r;
	}

	public Buffer makeBufferFromData(WaveData data)
		throws FileNotFoundException {

		System.out.println(" loaded :" + data.format + " " + data.samplerate
			+ " " + data.data);
		Buffer r = new Buffer();
		AL10.alGenBuffers(r.buffer);
		AL10.alBufferData(r.buffer.get(0), data.format, data.data,
			data.samplerate);
//		AL10.alBufferData(r.buffer.get(0), 0x1205, data.data,
//				data.samplerate);
		data.dispose();

		return r;
	}

	public Buffer makeBufferFromFloats(float[] data, int sampleRate)
		throws FileNotFoundException {

		ShortBuffer s = ByteBuffer.allocateDirect(data.length * 2)
			.order(ByteOrder.nativeOrder()).asShortBuffer();
		for (float f : data)
			s.put((short) (Math.min(Short.MAX_VALUE,
				Math.max(Short.MIN_VALUE, Short.MAX_VALUE * f))));
		s.rewind();
		Buffer r = new Buffer();
		AL10.alGenBuffers(r.buffer);
		AL10.alBufferData(r.buffer.get(0), AL10.AL_FORMAT_MONO16, s, sampleRate);
		return r;
	}

	public Buffer makeBufferFromFloats(float[] left, float[] right,
					   int sampleRate) throws FileNotFoundException {

		ShortBuffer s = ByteBuffer.allocateDirect(left.length * 4)
			.order(ByteOrder.nativeOrder()).asShortBuffer();

		for (int f = 0; f < left.length; f++) {
			s.put((short) (Math.min(Short.MAX_VALUE,
				Math.max(Short.MIN_VALUE, Short.MAX_VALUE * left[f]))));
			s.put((short) (Math.min(Short.MAX_VALUE,
				Math.max(Short.MIN_VALUE, Short.MAX_VALUE * right[f]))));
		}
		s.rewind();
		Buffer r = new Buffer();
		AL10.alGenBuffers(r.buffer);
		AL10.alBufferData(r.buffer.get(0), AL10.AL_FORMAT_STEREO16, s,
			sampleRate);
		return r;
	}

}
