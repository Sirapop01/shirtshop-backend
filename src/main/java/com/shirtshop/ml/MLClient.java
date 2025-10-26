package com.shirtshop.ml;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MLClient {

    private final WebClient mlWebClient;

    @Value("${ml.gradio.endpoint}")
    private String endpoint;

    @Value("${ml.gradio.mode:json}")
    private String mode; // json | multipart

    // ✅ แก้ error "Cannot resolve symbol 'jsonStyle'"
    @Value("${ml.gradio.json-style:array}")
    private String jsonStyle; // array | object

    // ----- defaults for inputs 3..7 -----
    @Value("${ml.gradio.description:Short Sleeve Round Neck T-shirts}")
    private String description;        // ช่อง 3 (Textbox)
    @Value("${ml.gradio.is-checked:true}")
    private boolean isChecked;         // ช่อง 4 (Checkbox)
    @Value("${ml.gradio.is-checked-crop:false}")
    private boolean isCheckedCrop;     // ช่อง 5 (Checkbox)
    @Value("${ml.gradio.denoise-steps:30}")
    private int denoiseSteps;          // ช่อง 6 (Number)
    @Value("${ml.gradio.seed:42}")
    private int seed;                  // ช่อง 7 (Number)
    // ------------------------------------

    public Mono<String> postJson(Map<String, Object> payload) {
        return mlWebClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(s -> s.isError(), resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new WebClientResponseException(
                                        "ML error: " + body,
                                        resp.statusCode().value(),
                                        resp.statusCode().toString(),
                                        resp.headers().asHttpHeaders(), null, null))))
                .bodyToMono(String.class);
    }

    public Mono<String> postMultipart(String personBase64, String garmentBase64) {
        byte[] personBytes = decodeBase64(personBase64);
        byte[] garmentBytes = decodeBase64(garmentBase64);

        ByteArrayResource personRes = new ByteArrayResource(personBytes) { @Override public String getFilename() { return "person.jpg"; } };
        ByteArrayResource garmentRes = new ByteArrayResource(garmentBytes) { @Override public String getFilename() { return "garment.jpg"; } };

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("person", personRes);
        form.add("garment", garmentRes);

        return mlWebClient.post()
                .uri(endpoint)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(form))
                .retrieve()
                .onStatus(s -> s.isError(),
                        resp -> resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new WebClientResponseException(
                                        "ML error: " + body,
                                        resp.statusCode().value(),
                                        resp.statusCode().toString(),
                                        resp.headers().asHttpHeaders(),
                                        null, null))))
                .bodyToMono(String.class);
    }

    public Mono<String> send(String personBase64, String garmentBase64, String optionsJson) {
        if ("multipart".equalsIgnoreCase(mode)) {
            // ถ้าจะเทสแบบ multipart ให้เขียน postMultipart(personBase64, garmentBase64) ตามที่มีอยู่เดิม
            throw new UnsupportedOperationException("Use JSON mode for /api/tryon");
        }

        // ถ้า FE ส่ง optionsJson มา ให้ใช้เป็น description ช่องที่ 3 แทนค่า default
        String desc = (optionsJson != null && !optionsJson.isBlank()) ? optionsJson : description;

        Object[] data = new Object[] {
                personBase64,   // 1) human_b64
                garmentBase64,  // 2) cloth_b64
                desc,           // 3) description (Textbox)
                isChecked,      // 4) True
                isCheckedCrop,  // 5) False
                denoiseSteps,   // 6) 30
                seed            // 7) 42
        };

        Map<String, Object> payload = Map.of("data", data);
        return postJson(payload);
    }

    // helper: (เผื่อคุณใช้ที่อื่น)
    private static byte[] decodeBase64(String possiblyDataUrl) {
        String pure = possiblyDataUrl;
        int comma = pure.indexOf(',');
        if (pure.startsWith("data:") && comma > 0) pure = pure.substring(comma + 1);
        return Base64.getDecoder().decode(pure);
    }
}
