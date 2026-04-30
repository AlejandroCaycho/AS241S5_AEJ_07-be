package reactive_app.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactive_app.model.IAResponse;
import reactor.core.publisher.Flux;

public interface IAResponseRepository extends ReactiveCrudRepository<IAResponse, Long> {

    Flux<IAResponse> findAllByOrderByFechaDesc();

    Flux<IAResponse> findBySource(String source);

    Flux<IAResponse> findTop6BySourceOrderByFechaDesc(String source);
}
