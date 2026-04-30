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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IAService {

    private static final String OPENROUTER_SOURCE = "openrouter";
    private static final String MISTRAL_SOURCE = "mistral";
    private static final int CONTEXT_LIMIT = 6;
    private static final String SYSTEM_PROMPT = """
            Eres un asistente de IA claro, preciso y util.
            Responde en espanol salvo que el usuario pida otro idioma.
            Usa el contexto de conversaciones anteriores cuando sea relevante.
            Si falta informacion, dilo y pregunta lo minimo necesario.
            Evita inventar datos y separa la respuesta en pasos o bullets cuando mejore la claridad.
            No uses emojis, emoticones ni simbolos decorativos en tus respuestas.
            """;

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
        String normalizedPrompt = prompt == null ? "" : prompt.trim();

        if (normalizedPrompt.isBlank()) {
            return Flux.error(new IllegalArgumentException("El prompt no puede estar vacio."));
        }

        Mono<IAResponse> desdeOpenRouter = consultarOpenRouter(normalizedPrompt)
                .flatMap(respuesta -> guardarRespuesta(OPENROUTER_SOURCE, openRouterModel, normalizedPrompt, respuesta));

        Mono<IAResponse> desdeMistral = consultarMistral(normalizedPrompt)
                .flatMap(respuesta -> guardarRespuesta(MISTRAL_SOURCE, mistralModel, normalizedPrompt, respuesta));

        return Flux.merge(desdeOpenRouter, desdeMistral);
    }

    private Mono<String> consultarOpenRouter(String prompt) {
        return obtenerMensajesConContexto(OPENROUTER_SOURCE, prompt)
                .flatMap(messages -> {
                    Map<String, Object> body = Map.of(
                            "model", openRouterModel,
                            "messages", messages,
                            "temperature", 0.4
                    );

                    return webClient.post()
                            .uri(openRouterUrl + "/chat/completions")
                            .header("Authorization", "Bearer " + openRouterKey)
                            .header("HTTP-Referer", "http://localhost:8080")
                            .header("X-Title", "reactive-app")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(body)
                            .retrieve()
                            .onStatus(
                                    status -> status.is4xxClientError() || status.is5xxServerError(),
                                    response -> response.bodyToMono(String.class)
                                            .flatMap(errorBody -> Mono.error(
                                                    new RuntimeException("OpenRouter error " + response.statusCode() + ": " + errorBody)
                                            ))
                            )
                            .bodyToMono(Map.class)
                            .map(this::extraerContenido)
                            .onErrorResume(e -> Mono.just("OpenRouter no disponible: " + e.getMessage()));
                });
    }

    private Mono<String> consultarMistral(String prompt) {
        return obtenerMensajesConContexto(MISTRAL_SOURCE, prompt)
                .flatMap(messages -> {
                    Map<String, Object> body = Map.of(
                            "model", mistralModel,
                            "messages", messages,
                            "temperature", 0.4
                    );

                    return webClient.post()
                            .uri(mistralUrl + "/chat/completions")
                            .header("Authorization", "Bearer " + mistralKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(body)
                            .retrieve()
                            .onStatus(
                                    status -> status.is4xxClientError() || status.is5xxServerError(),
                                    response -> response.bodyToMono(String.class)
                                            .flatMap(errorBody -> Mono.error(
                                                    new RuntimeException("Mistral error " + response.statusCode() + ": " + errorBody)
                                            ))
                            )
                            .bodyToMono(Map.class)
                            .map(this::extraerContenido)
                            .onErrorResume(e -> Mono.just("Mistral no disponible: " + e.getMessage()));
                });
    }

    private Mono<List<Map<String, String>>> obtenerMensajesConContexto(String source, String prompt) {
        return repository.findTop6BySourceOrderByFechaDesc(source)
                .collectList()
                .map(historial -> {
                    Collections.reverse(historial);

                    List<Map<String, String>> messages = new ArrayList<>();
                    messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

                    historial.stream()
                            .filter(item -> item.getPrompt() != null && item.getRespuestaIa() != null)
                            .limit(CONTEXT_LIMIT)
                            .forEach(item -> {
                                messages.add(Map.of("role", "user", "content", item.getPrompt()));
                                messages.add(Map.of("role", "assistant", "content", item.getRespuestaIa()));
                            });

                    messages.add(Map.of("role", "user", "content", prompt));
                    return messages;
                });
    }

    private String extraerContenido(Map response) {
        var choices = (List<Map>) response.get("choices");
        var message = (Map) choices.get(0).get("message");
        return limpiarEmojis((String) message.get("content"));
    }

    private String limpiarEmojis(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("[\\x{1F300}-\\x{1FAFF}\\x{2600}-\\x{27BF}]", "").trim();
    }

    private Mono<IAResponse> guardarRespuesta(String source, String model, String prompt, String respuesta) {
        IAResponse data = new IAResponse();
        data.setSource(source);
        data.setModel(model);
        data.setPrompt(prompt);
        data.setRespuestaIa(respuesta);
        data.setFecha(LocalDateTime.now());
        return repository.save(data);
    }

    public Flux<IAResponse> historial() {
        return repository.findAllByOrderByFechaDesc();
    }

    public Flux<IAResponse> historialPorFuente(String source) {
        return repository.findBySource(source);
    }
}
