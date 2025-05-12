package org.example.cdrservice.entitites;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность "Запись данных о вызове" (Call Data Record - CDR).
 * <p>
 * Содержит подробную информацию об одной телекоммуникационной транзакции,
 * такой как телефонный звонок. Включает тип вызова, участвующих абонентов,
 * время начала и окончания, а также статус обработки.
 * </p>
 *
 * @author Сервис роуминговой агрегации
 * @since 1.0
 */
@Entity
@Table(name = "cdrs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cdr {

    /**
     * Уникальный идентификатор записи CDR.
     * <p>
     * Автоматически генерируется базой данных при сохранении.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Тип вызова.
     */
    @Column(name = "call_type", nullable = false)
    private String callType;

    /**
     * MSISDN (номер телефона) обслуживаемого абонента или абонента, которому выставляется счет за этот вызов.
     */
    @Column(name = "serviced_msisdn", nullable = false)
    private String servicedMsisdn;

    /**
     * MSISDN (номер телефона) другой стороны, участвующей в вызове.
     */
    @Column(name = "other_msisdn", nullable = false)
    private String otherMsisdn;

    /**
     * Дата и время начала вызова.
     */
    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    /**
     * Дата и время окончания вызова.
     */
    @Column(name = "finish_date_time", nullable = false)
    private LocalDateTime finishDateTime;

    /**
     * Статус обработки этой CDR (например, NEW, CONSUMED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "consumed_status", nullable = false)
    private ConsumedStatus consumedStatus;

}
