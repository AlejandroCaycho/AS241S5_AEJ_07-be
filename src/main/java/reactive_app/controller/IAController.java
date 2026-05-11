package reactive_app.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactive_app.model.IAResponse;
import reactive_app.service.IAService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin("*")
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

    @GetMapping("/historial/fuente/{source}")
    public Flux<IAResponse> historialPorFuente(@PathVariable String source) {
        return iaService.historialPorFuente(source);
    }

    @PutMapping("/historial/{id}")
    public Flux<IAResponse> actualizarPrompt(
            @PathVariable Long id,
            @RequestBody PromptRequest request) {
        return iaService.actualizarPrompt(id, request.getPrompt());
    }

    @DeleteMapping("/historial/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> eliminarPorId(@PathVariable Long id) {
        return iaService.eliminarPorId(id);
    }

    @DeleteMapping("/historial")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> eliminarTodo() {
        return iaService.eliminarTodo();
    }
}