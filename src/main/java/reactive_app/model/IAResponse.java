package reactive_app.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("ai_responses")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IAResponse {

    @Id
    private Long id;

    private String source;
    private String model;

    private String prompt;

    @Column("response")
    private String respuestaIa;

    @Column("tokens_used")
    private Integer tokensUsed;

    @Column("created_at")
    private LocalDateTime fecha;
}