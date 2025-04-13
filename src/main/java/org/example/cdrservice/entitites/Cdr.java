package org.example.cdrservice.entitites;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Сущность "Запись данных вызова" (Call Data Record - CDR).
 * <p>
 * Содержит информацию об одном телефонном звонке между двумя абонентами,
 * включая время начала и окончания звонка, типа звонка, а также
 * номера вызывающего и вызываемого абонентов.
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
public class Cdr implements Serializable {

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
     * <p>
     * 01 - входящий вызов, 02 - исходящий вызов.
     * </p>
     */
    @Column(name = "call_type", nullable = false)
    private String callType;

    /**
     * Номер телефона вызывающего абонента.
     */
    @Column(name = "caller_number", nullable = false)
    private String callerNumber;

    /**
     * Номер телефона вызываемого абонента.
     */
    @Column(name = "called_number", nullable = false)
    private String calledNumber;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "consumed_status", nullable = false)
    private ConsumedStatus consumedStatus;
}
