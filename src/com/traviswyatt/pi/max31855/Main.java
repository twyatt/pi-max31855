package com.traviswyatt.pi.max31855;

import java.util.ArrayList;
import java.util.List;

import com.pi4j.wiringpi.Spi;


public class Main {
	
	static List<String> faults = new ArrayList<String>();
	
	public static void main(String[] args) {
		// https://projects.drogon.net/understanding-spi-on-the-raspberry-pi/
		// http://developer-blog.net/wp-content/uploads/2013/09/raspberry-pi-rev2-gpio-pinout.jpg
		// http://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus
		
		int channel = Spi.CHANNEL_0;
		int fd = Spi.wiringPiSPISetup(channel, 500000); // 500 kHz
		if (fd == -1) {
			throw new RuntimeException("SPI setup failed.");
		}
		
		// http://pi4j.com/example/control.html
		MAX31855 max31855 = new MAX31855(channel);
		
		int[] raw = new int[2];
		while (true) {
			int faults = max31855.readRaw(raw);
			
			float internal = max31855.getInternalTemperature(raw[0]);
			float thermocouple = max31855.getThermocoupleTemperature(raw[1]);
			
			System.out.println("Internal = " + internal + " C, Thermocouple = " + thermocouple + " C");
			if (faults != 0) {
				onFaults(faults);
			}
		}
	}

	private static void onFaults(int f) {
		faults.clear();
		
		if ((f & MAX31855.FAULT_OPEN_CIRCUIT_BIT) == MAX31855.FAULT_OPEN_CIRCUIT_BIT)
			faults.add("Open Circuit");
		if ((f & MAX31855.FAULT_SHORT_TO_GND_BIT) == MAX31855.FAULT_SHORT_TO_GND_BIT)
			faults.add("Short To GND");
		if ((f & MAX31855.FAULT_SHORT_TO_VCC_BIT) == MAX31855.FAULT_SHORT_TO_VCC_BIT)
			faults.add("Short To VCC");

		boolean first = true;
		String text = "Faults = ";
		for (String fault : faults) {
			if (!first)
				text += ", ";
			text += fault;
		}
		
		System.err.println(text);
	}

}
