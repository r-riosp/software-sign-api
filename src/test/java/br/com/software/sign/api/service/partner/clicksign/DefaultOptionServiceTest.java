package br.com.software.sign.api.service.partner.clicksign;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DefaultOptionServiceTest {

    private final DefaultOptionService service = new DefaultOptionService();

    @Test
    void testGetActionWithNullReturnsDefault() {
        String result = service.getAction(null);
        assertEquals("sign", result);
    }

    @Test
    void testGetActionWithEmptyStringReturnsDefault() {
        String result = service.getAction("");
        assertEquals("sign", result);
    }

    @Test
    void testGetActionWithBlankStringReturnsDefault() {
        String result = service.getAction("   ");
        assertEquals("sign", result);
    }

    @Test
    void testGetActionWithApproveReturnsApprove() {
        String result = service.getAction("approve");
        assertEquals("approve", result);
    }

    @Test
    void testGetActionWithWitnessReturnsWitness() {
        String result = service.getAction("witness");
        assertEquals("witness", result);
    }

    @Test
    void testGetActionWithSignReturnsSign() {
        String result = service.getAction("sign");
        assertEquals("sign", result);
    }
}
