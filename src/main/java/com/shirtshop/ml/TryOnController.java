package com.shirtshop.ml;

import com.shirtshop.ml.dto.TryOnRequest;
import com.shirtshop.ml.dto.TryOnResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tryon")
@RequiredArgsConstructor
public class TryOnController {

    private final TryOnService tryOnService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<TryOnResponse> tryOn(@RequestBody @Valid TryOnRequest req) {
        return tryOnService.tryOn(req);
    }
}
