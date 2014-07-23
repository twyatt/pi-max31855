package com.traviswyatt.pi.max31855;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.wiringpi.Spi;

public class MAX31855 {
	
	private static final int READ_BUFFER_SIZE = 4; // bytes

	public static final int THERMOCOUPLE_SIGN_BIT = 0x80000000; // D31
	public static final int INTERNAL_SIGN_BIT     = 0x8000;     // D15

	public static final int FAULT_BIT = 0x10000; // D16

	public static final byte FAULT_OPEN_CIRCUIT_BIT = (byte) 0x01;
	public static final byte FAULT_SHORT_TO_GND_BIT = (byte) 0x02;
	public static final byte FAULT_SHORT_TO_VCC_BIT = (byte) 0x04;

	/**
	 * 11 of the least most significant bits (big endian) set to 1.
	 */
	public static final int LSB_11 = 0x07FF;

	/**
	 * 13 of the least most significant bits (big endian) set to 1.
	 */
	public static final int LSB_13 = 0x1FFF;

	public interface MAX31855Listener {
		public void onData(float internal /* C */, float thermocouple /* C */);
		public void onFault(byte fault);
	}

	private MAX31855Listener listener;

	private volatile float internal;
	private volatile float thermocouple;

	private final int channel;
	private final Pin ss;

	private GpioPinDigitalOutput slaveSelect;
	
	private byte[] readBuffer  = new byte[READ_BUFFER_SIZE];

	public MAX31855(int channel, Pin ss) {
		this.channel = channel;
		this.ss = ss;
	}

	public MAX31855 setListener(MAX31855Listener listener) {
		this.listener = listener;
		return this;
	}

	protected void read(byte[] data) {
		slaveSelect.setState(PinState.LOW);
		Spi.wiringPiSPIDataRW(channel, data, data.length);
		slaveSelect.setState(PinState.HIGH);
	}

	public void setup() {
		GpioController gpio = GpioFactory.getInstance();
		slaveSelect = gpio.provisionDigitalOutputPin(ss, PinState.HIGH);
	}

	public void loop() {
		// clear buffer
		for (int i = 0; i < readBuffer.length; i++) {
			readBuffer[i] = 0;
		}
		
		read(readBuffer);
		int data = ((readBuffer[0] & 0xFF) << 24) |
				   ((readBuffer[1] & 0xFF) << 16) |
				   ((readBuffer[2] & 0xFF) <<  8) |
				   ((readBuffer[3] & 0xFF) <<  0);

		if ((data & FAULT_BIT) == FAULT_BIT) {
			if (listener != null) {
				listener.onFault((byte) (data & 0x07));
			}
		}

		int internal = (int) ((data >> 4) & LSB_11);
		if ((data & INTERNAL_SIGN_BIT) == INTERNAL_SIGN_BIT) {
			internal = -(~internal & LSB_11);
		}

		int thermocouple = (int) ((data >> 18) & LSB_13);
		if ((data & THERMOCOUPLE_SIGN_BIT) == THERMOCOUPLE_SIGN_BIT) {
			thermocouple = -(~thermocouple & LSB_13);
		}

		this.internal = internal * 0.0625f;
		this.thermocouple = thermocouple * 0.25f;

		if (listener != null) {
			listener.onData(this.internal, this.thermocouple);
		}
	}
	
}
