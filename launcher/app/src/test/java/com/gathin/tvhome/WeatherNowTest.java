package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class WeatherNowTest {
    private static final WeatherNow.Words ES =
            new WeatherNow.Words("Humedad %1$d %%", "Lluvia %1$d %%", "Nieve %1$d %%", "%1$d %%", "lluvia", "nieve");
    private static final WeatherNow.Words EN =
            new WeatherNow.Words("Humidity %1$d%%", "Rain %1$d%%", "Snow %1$d%%", "%1$d%%", "rain", "snow");

    private static Object[] row(long code, Object sky, Object humidity, Object rain, Object snow) {
        return new Object[] {"Madrid", 14L, "C", code, 1L, 1_000L, sky, humidity, rain, snow};
    }

    private static String line(Object[] row, WeatherNow.Words w) {
        return WeatherNow.fromRow(row).text(w, false);
    }

    @Test
    public void symbolsMatchHeaderWeather() {
        assertEquals("☀︎", WeatherNow.sky(0, true));
        assertEquals("☾", WeatherNow.sky(1, false));
        assertEquals("⛅︎", WeatherNow.sky(2, true));
        assertEquals("⛅︎", WeatherNow.sky(2, false));
        assertEquals("☁︎", WeatherNow.sky(3, true));
        assertEquals("≡", WeatherNow.sky(45, true));
        assertEquals("☂︎", WeatherNow.sky(53, false));
        assertEquals("☔︎", WeatherNow.sky(61, true));
        assertEquals("☔︎", WeatherNow.sky(81, true));
        assertEquals("❄︎", WeatherNow.sky(73, true));
        assertEquals("⚡︎", WeatherNow.sky(95, true));
        assertEquals("", WeatherNow.sky(42, true));
        assertEquals("", WeatherNow.sky(-1, true));
    }

    @Test
    public void sixColumnsOnlyDrawAsBefore() {
        Object[] six = {"Madrid", 14L, "C", 3L, 1L, 1_000L};
        assertEquals("14° · Madrid", WeatherNow.fromRow(six).text(ES, true));
        assertEquals("14°", WeatherNow.fromRow(six).text(ES, false));
    }

    @Test
    public void missingPartsAreDropped() {
        assertEquals("14°", line(row(3, " ", null, null, null), EN));
        assertEquals("14° · Cloudy · Humidity 90%", line(row(3, "Cloudy", 90L, null, null), EN));
        assertEquals("14° · Nublado · Humedad 90 %", line(row(3, "Nublado", 90L, null, null), ES));
    }

    @Test
    public void outOfRangeIsDroppedNotClamped() {
        assertEquals("14°", line(row(3, null, 140L, 101L, -3L), EN));
        assertEquals("14°", line(row(3, null, -5L, null, null), EN));
    }

    @Test
    public void chanceShowsFromThirtyUp() {
        assertEquals("14° · Nublado", line(row(3, "Nublado", null, 0L, null), ES));
        assertEquals("14° · Nublado", line(row(3, "Nublado", null, 29L, null), ES));
        assertEquals("14° · Nublado · Lluvia 30 %", line(row(3, "Nublado", null, 30L, null), ES));
        assertEquals("14° · Overcast · Rain 60% · Humidity 57%", line(row(3, "Overcast", 57L, 60L, null), EN));
    }

    @Test
    public void theWordIsNotRepeated() {
        assertEquals("14° · Lluvia débil · 80 % · Humedad 90 %", line(row(61, "Lluvia débil", 90L, 80L, null), ES));
        assertEquals("1° · Snow · 70%", line(new Object[] {"", 1L, "C", 73L, 1L, 1_000L, "Snow", null, 70L, 70L}, EN));
    }

    @Test
    public void snowIsDecidedByTheCodeInAnyLanguage() {
        // "Nevada fuerte" does not say "nieve", but code 75 is snow: no "Nieve" again, and no rain figure.
        assertEquals("1° · Nevada fuerte · 70 %", line(new Object[] {"", 1L, "C", 75L, 1L, 1_000L, "Nevada fuerte", null, 90L, 70L}, ES));
        assertEquals("1° · Nevada fuerte", line(new Object[] {"", 1L, "C", 75L, 1L, 1_000L, "Nevada fuerte", null, 90L, null}, ES));
    }

    @Test
    public void snowWinsOverRain() {
        assertEquals("14° · Nublado · Nieve 40 %", line(row(3, "Nublado", null, 80L, 40L), ES));
    }

    @Test
    public void noTemperatureOrTimeIsNoReading() {
        Object[] r = row(3, null, null, null, null);
        r[1] = null;
        assertNull(WeatherNow.fromRow(r));
        r = row(3, null, null, null, null);
        r[5] = null;
        assertNull(WeatherNow.fromRow(r));
    }
}
