package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class WeatherNowTest {
    private static Object[] row(Object sky, Object humidity) {
        return new Object[] {"Madrid", 14L, "C", 3L, 1L, 1_000L, sky, humidity};
    }

    @Test
    public void symbolsMatchHeaderWeather() {
        assertEquals("≡", WeatherNow.sky(45, true));
        assertEquals("☔︎", WeatherNow.sky(61, true));
        assertEquals("☔︎", WeatherNow.sky(53, false));
        assertEquals("❄︎", WeatherNow.sky(73, true));
        assertEquals("⚡︎", WeatherNow.sky(95, true));
    }

    @Test
    public void missingPartsAreDropped() {
        assertEquals("14° · Madrid", WeatherNow.fromRow(row(null, null)).text("Humidity %d%%", true));
        assertEquals("14°", WeatherNow.fromRow(row(" ", null)).text("Humidity %d%%", false));
        assertEquals("14° · Cloudy · Humidity 90%", WeatherNow.fromRow(row("Cloudy", 90L)).text("Humidity %d%%", false));
    }

    @Test
    public void humidityOutOfRangeIsDroppedNotClamped() {
        assertEquals("14°", WeatherNow.fromRow(row(null, 140L)).text("Humidity %d%%", false));
        assertEquals("14°", WeatherNow.fromRow(row(null, -5L)).text("Humidity %d%%", false));
    }

    @Test
    public void noTemperatureOrTimeIsNoReading() {
        Object[] r = row(null, null);
        r[1] = null;
        assertNull(WeatherNow.fromRow(r));
        r = row(null, null);
        r[5] = null;
        assertNull(WeatherNow.fromRow(r));
    }
}
