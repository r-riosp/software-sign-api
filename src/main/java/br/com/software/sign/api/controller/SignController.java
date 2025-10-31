package br.com.software.sign.api.controller;

import br.com.software.sign.api.dto.*;
import br.com.software.sign.api.service.SignService;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.engine.spi.IdentifierValue;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@Validated
public class SignController {

    private final SignService app;

    @Autowired
    public SignController(SignService app) {
        this.app = app;
    }

    @PostMapping(value = "/send", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> send(@RequestParam(required = false) String name,
                                  @RequestBody @Valid SignSendRequest req) {
        return ResponseEntity.ok(app.send(name, req));
    }

    @PostMapping(value = "/send/batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendBatch(@RequestParam(required = false) String name,
                                       @RequestBody @Valid List<SignSendRequest> reqs) {
        List<Object> results = reqs.stream()
                .map(r -> app.send(name, r))
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @PostMapping(value = "/envelopes", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createEnvelope(@RequestBody EnvelopeCreateRequestDTO body) {
        return ResponseEntity.status(201).body(app.createEnvelope(body));
    }

    @PostMapping(value = "/envelopes/{envelopeId}/documents",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadDocument(@PathVariable String envelopeId,
                                            @RequestBody @Valid UploadDocumentRequestDTO req) throws IOException {
        return ResponseEntity.ok(app.uploadDocument(envelopeId, req));
    }

    @PostMapping(value = "/envelopes/{envelopeId}/signers",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addSigners(@PathVariable String envelopeId, @RequestBody JsonNode body) {
        return ResponseEntity.ok(app.addSigners(envelopeId, body));
    }

    @PostMapping(value = "/envelopes/{envelopeId}/requirements",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addRequirements(@PathVariable String envelopeId,
                                             @RequestBody AddRequirementsRequestDTO body) {
        return ResponseEntity.status(201).body(app.addRequirements(envelopeId, body));
    }

    @PostMapping(value = "/envelopes/{envelopeId}/notifications",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> notifyAll(@PathVariable String envelopeId,
                                       @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(app.notifyAll(envelopeId, body));
    }

    @PostMapping(value = "/envelopes/{envelopeId}/signers/{signerId}/notifications",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> notifyOne(@PathVariable String envelopeId,
                                       @PathVariable String signerId,
                                       @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(app.notifyOne(envelopeId, signerId, body));
    }

    @GetMapping(value = "/envelopes/{envelopeId}/documents", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listEnvelopeDocuments(@PathVariable String envelopeId,
                                                   @RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer size,
                                                   @RequestParam(required = false) String tenant,
                                                   @RequestParam(defaultValue = "true") boolean signedBase64) {
        return ResponseEntity.ok(app.listEnvelopeDocuments(envelopeId, page, size, tenant, signedBase64));
    }

    @PatchMapping(value = "/envelopes/{envelopeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> activateEnvelope(@PathVariable String envelopeId) {
        return ResponseEntity.ok(app.activateEnvelope(envelopeId));
    }
}