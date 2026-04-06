package reactive_app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactive_app.model.IAResponse;
import reactive_app.repository.IAResponseRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IAService {

    private final IAResponseRepository repository;
    private final WebClient webClient = WebClient.create();

    @Value("${api.openrouter.url}")
    private String openRouterUrl;

    @Value("${api.openrouter.key}")
    private String openRouterKey;

    @Value("${api.openrouter.model}")
    private String openRouterModel;

    @Value("${api.mistral.url}")
    private String mistralUrl;

    @Value("${api.mistral.key}")
    private String mistralKey;

    @Value("${api.mistral.model}")
    private String mistralModel;

    public Flux<IAResponse> procesar(String prompt) {
        Mono<IAResponse> desdeOpenRouter = consultarOpenRouter(prompt)
                .flatMap(respuesta -> {
                    IAResponse data = new IAResponse();
                    data.setSource("openrouter");
                    data.setModel(openRouterModel);
                    data.setPrompt(prompt);
                    data.setRespuestaIa(respuesta);
                    data.setFecha(LocalDateTime.now());
                    return repository.save(data);
                });

        Mono<IAResponse> desdeMistral = consultarMistral(prompt)
                .flatMap(respuesta -> {
                    IAResponse data = new IAResponse();
                    data.setSource("mistral");
                    data.setModel(mistralModel);
                    data.setPrompt(prompt);
                    data.setRespuestaIa(respuesta);
                    data.setFecha(LocalDateTime.now());
                    return repository.save(data);
                });

        return Flux.merge(desdeOpenRouter, desdeMistral);
    }

    private Mono<String> consultarOpenRouter(String prompt) {
        Map<String, Object> body = Map.of(
                "model", openRouterModel,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        return webClient.post()
                .uri(openRouterUrl + "/chat/completions")
                .header("Authorization", "Bearer " + openRouterKey)
                .header("HTTP-Referer", "http://localhost:8080")
                .header("X-Title", "reactive-app")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(res -> {
                    var choices = (List<Map>) res.get("choices");
                    var message = (Map) choices.get(0).get("message");
                    return (String) message.get("content");
                })
                .onErrorResume(e -> Mono.just("OpenRouter no disponible: " + e.getMessage()));
    }

    private Mono<String> consultarMistral(String prompt) {
        Map<String, Object> body = Map.of(
                "model", mistralModel,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        return webClient.post()
                .uri(mistralUrl + "/chat/completions")
                .header("Authorization", "Bearer " + mistralKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(res -> {
                    var choices = (List<Map>) res.get("choices");
                    var message = (Map) choices.get(0).get("message");
                    return (String) message.get("content");
                })
                .onErrorResume(e -> Mono.just("Mistral no disponible: " + e.getMessage()));
    }

    public Flux<IAResponse> historial() {
        return repository.findAllByOrderByFechaDesc();
    }

    public Flux<IAResponse> historialPorFuente(String source) {
        return repository.findBySource(source);
    }
}