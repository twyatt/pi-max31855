package com.traviswyatt.pi.max31855;

import java.util.ArrayList;
import java.util.List;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.Spi;


public class Main {

	/**
	 * The SPI kernel module is required to enable the SPI driver:
	 * $ sudo modprobe spi_bcm2708
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// https://projects.drogon.net/understanding-spi-on-the-raspberry-pi/
		// http://pi4j.com/example/control.html
		// http://developer-blog.net/wp-content/uploads/2013/09/raspberry-pi-rev2-gpio-pinout.jpg
		// http://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus
		
		int channel = 0;
		int fd = Spi.wiringPiSPISetup(channel, 500000); // 0.5 MHz
		if (fd == -1) {
			throw new RuntimeException("SPI setup failed.");
		}
		
		MAX31855 max31855 = new MAX31855(channel, RaspiPin.GPIO_01);
		max31855.setListener(new MAX31855.MAX31855Listener() {
			List<String> faults = new ArrayList<String>();
			
			@Override
			public void onFault(byte f) {
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
			
			@Override
			public void onData(float internal, float thermocouple) {
				System.out.println("Internal = " + internal + " C, Thermocouple = " + thermocouple + " C");
			}
		});
		
		max31855.setup();
		
		try {
			while (true) {
				max31855.loop();
				Thread.sleep(20L);
			}
		} catch (InterruptedException e) {
			GpioController gpio = GpioFactory.getInstance();
			gpio.shutdown();
		}
	}

}
