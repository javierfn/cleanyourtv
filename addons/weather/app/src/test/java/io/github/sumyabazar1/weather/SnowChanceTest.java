package io.github.sumyabazar1.weather;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** snow_chance for Home: the chance of precipitation, claimed as snow only when the reading says snow. */
public class SnowChanceTest {
    @Test
    public void onlyWhenTheReadingSaysSnow() {
        assertEquals(70, Forecast.snowChance(73, 70, 0));
        assertEquals(70, Forecast.snowChance(86, 70, 0));
        assertEquals(40, Forecast.snowChance(3, 40, 0.2));
        assertEquals(-1, Forecast.snowChance(61, 80, 0));
        assertEquals(-1, Forecast.snowChance(0, 0, 0));
        assertEquals(-1, Forecast.snowChance(73, -1, 0.5));
    }

    @Test
    public void coldRainCountsAsSnow() {
        // At 0 °C and 90 % the add-on already turns light rain into light snow, so the chance is a snow chance.
        assertEquals(60, Forecast.snowChance(Forecast.snowIfCold(61, 0, 90), 60, 0));
    }
}
