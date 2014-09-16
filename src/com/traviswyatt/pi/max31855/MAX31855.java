package com.traviswyatt.pi.max31855;

import java.util.Arrays;

import com.pi4j.wiringpi.Spi;

public class MAX31855 {
	
	public static final int THERMOCOUPLE_SIGN_BIT = 0x80000000; // D31
	public static final int INTERNAL_SIGN_BIT     = 0x8000;     // D15

	public static final int FAULT_BIT = 0x10000; // D16

	public static final byte FAULT_OPEN_CIRCUIT_BIT = 0x01; // D0
	public static final byte FAULT_SHORT_TO_GND_BIT = 0x02; // D1
	public static final byte FAULT_SHORT_TO_VCC_BIT = 0x04; // D2

	/**
	 * 11 of the least most significant bits (big endian) set to 1.
	 */
	public static final int LSB_11 = 0x07FF;

	/**
	 * 13 of the least most significant bits (big endian) set to 1.
	 */
	public static final int LSB_13 = 0x1FFF;

	private final byte[] BUFFER = new byte[4];
	
	private final int channel;

	public MAX31855(int channel) {
		this.channel = channel;
	}

	/**
	 * Read raw temperature data.
	 * 
	 * @param raw Array of raw temperatures whereas index 0 = internal, 1 = themocouple
	 * @return Returns any faults or 0 if there were no faults
	 */
	public int readRaw(int[] raw) {
		if (raw.length != 2)
			throw new IllegalArgumentException("Temperature array must have a length of 2");
		
		// http://stackoverflow.com/a/9128762/196486
		Arrays.fill(BUFFER, (byte) 0); // clear buffer
		
		Spi.wiringPiSPIDataRW(channel, BUFFER, 4);
		
		int data = ((BUFFER[0] & 0xFF) << 24) |
				   ((BUFFER[1] & 0xFF) << 16) |
				   ((BUFFER[2] & 0xFF) <<  8) |
				   (BUFFER[3] & 0xFF);

		int internal = (int) ((data >> 4) & LSB_11);
		if ((data & INTERNAL_SIGN_BIT) == INTERNAL_SIGN_BIT) {
			internal = -(~internal & LSB_11);
		}

		int thermocouple = (int) ((data >> 18) & LSB_13);
		if ((data & THERMOCOUPLE_SIGN_BIT) == THERMOCOUPLE_SIGN_BIT) {
			thermocouple = -(~thermocouple & LSB_13);
		}

		raw[0] = internal;
		raw[1] = thermocouple;
		
		if ((data & FAULT_BIT) == FAULT_BIT) {
			return data & 0x07;
		} else {
			return 0; // no faults
		}
	}
	
	/**
	 * Converts raw internal temperature to actual internal temperature.
	 * 
	 * @param raw Raw internal temperature
	 * @return Actual internal temperature (C)
	 */
	public float getInternalTemperature(int raw) {
		return raw * 0.0625f;
	}
	
	/**
	 * Converts raw thermocouple temperature to actual thermocouple temperature.
	 * 
	 * @param raw Raw thermocouple temperature
	 * @return Actual thermocouple temperature (C)
	 */
	public float getThermocoupleTemperature(int raw) {
		return raw * 0.25f;
	}
	
}
