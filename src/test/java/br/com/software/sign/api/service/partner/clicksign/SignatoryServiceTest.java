package br.com.software.sign.api.service.partner.clicksign;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SignatoryServiceTest {

    @Mock
    private ClickSignService clickSignService;

    @Mock
    private DefaultOptionService defaultOptionService;

    private SignatoryService signatoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        signatoryService = new SignatoryService(clickSignService, defaultOptionService);
    }

    @Test
    void testAddSignerAppliesDefaultSignAs() {
        // Arrange
        String envelopeId = "test-envelope-123";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "test@example.com");
        attributes.put("name", "Test User");

        when(defaultOptionService.getSignAs(null)).thenReturn("sign");
        when(clickSignService.postEnvelopeSigners(anyString(), anyMap())).thenReturn(Map.of("data", Map.of("id", "signer-123")));

        // Act
        signatoryService.addSigner(envelopeId, attributes);

        // Assert
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(clickSignService).postEnvelopeSigners(eq(envelopeId), payloadCaptor.capture());

        Map<String, Object> capturedPayload = payloadCaptor.getValue();
        Map<String, Object> data = (Map<String, Object>) capturedPayload.get("data");
        Map<String, Object> capturedAttributes = (Map<String, Object>) data.get("attributes");

        assertEquals("sign", capturedAttributes.get("sign_as"));
    }

    @Test
    void testAddSignerPreservesCustomSignAs() {
        // Arrange
        String envelopeId = "test-envelope-123";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "test@example.com");
        attributes.put("name", "Test User");
        attributes.put("sign_as", "approve");

        when(clickSignService.postEnvelopeSigners(anyString(), anyMap())).thenReturn(Map.of("data", Map.of("id", "signer-123")));

        // Act
        signatoryService.addSigner(envelopeId, attributes);

        // Assert
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(clickSignService).postEnvelopeSigners(eq(envelopeId), payloadCaptor.capture());

        Map<String, Object> capturedPayload = payloadCaptor.getValue();
        Map<String, Object> data = (Map<String, Object>) capturedPayload.get("data");
        Map<String, Object> capturedAttributes = (Map<String, Object>) data.get("attributes");

        assertEquals("approve", capturedAttributes.get("sign_as"));
        verify(defaultOptionService, never()).getSignAs(any());
    }

    @Test
    void testAddSignerWithWitnessRole() {
        // Arrange
        String envelopeId = "test-envelope-123";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "witness@example.com");
        attributes.put("name", "Witness User");
        attributes.put("sign_as", "witness");

        when(clickSignService.postEnvelopeSigners(anyString(), anyMap())).thenReturn(Map.of("data", Map.of("id", "signer-123")));

        // Act
        signatoryService.addSigner(envelopeId, attributes);

        // Assert
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(clickSignService).postEnvelopeSigners(eq(envelopeId), payloadCaptor.capture());

        Map<String, Object> capturedPayload = payloadCaptor.getValue();
        Map<String, Object> data = (Map<String, Object>) capturedPayload.get("data");
        Map<String, Object> capturedAttributes = (Map<String, Object>) data.get("attributes");

        assertEquals("witness", capturedAttributes.get("sign_as"));
    }

    @Test
    void testAddSignerThrowsExceptionWhenEnvelopeIdIsNull() {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "test@example.com");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            signatoryService.addSigner(null, attributes);
        });
    }

    @Test
    void testAddSignerThrowsExceptionWhenEnvelopeIdIsBlank() {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "test@example.com");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            signatoryService.addSigner("", attributes);
        });
    }
}
