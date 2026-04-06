package reactive_app.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactive_app.model.IAResponse;
import reactive_app.service.IAService;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ia")
@RequiredArgsConstructor
public class IAController {

    private final IAService iaService;

    @PostMapping("/procesar")
    public Flux<IAResponse> procesar(@RequestBody PromptRequest request) {
        return iaService.procesar(request.getPrompt());
    }

    @GetMapping("/historial")
    public Flux<IAResponse> historial() {
        return iaService.historial();
    }

    @GetMapping("/historial/{source}")
    public Flux<IAResponse> historialPorFuente(@PathVariable String source) {
        return iaService.historialPorFuente(source);
    }
}