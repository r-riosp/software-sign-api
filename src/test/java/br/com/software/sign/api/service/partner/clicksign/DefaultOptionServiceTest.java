package br.com.software.sign.api.service.partner.clicksign;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DefaultOptionServiceTest {

    private final DefaultOptionService service = new DefaultOptionService();

    @Test
    void testGetSignAsWithNullReturnsDefault() {
        String result = service.getSignAs(null);
        assertEquals("sign", result);
    }

    @Test
    void testGetSignAsWithEmptyStringReturnsDefault() {
        String result = service.getSignAs("");
        assertEquals("sign", result);
    }

    @Test
    void testGetSignAsWithBlankStringReturnsDefault() {
        String result = service.getSignAs("   ");
        assertEquals("sign", result);
    }

    @Test
    void testGetSignAsWithCustomValueReturnsCustomValue() {
        String result = service.getSignAs("approve");
        assertEquals("approve", result);
    }

    @Test
    void testGetSignAsWithWitnessReturnsWitness() {
        String result = service.getSignAs("witness");
        assertEquals("witness", result);
    }

    @Test
    void testGetSignAsWithSignReturnsSign() {
        String result = service.getSignAs("sign");
        assertEquals("sign", result);
    }
}
