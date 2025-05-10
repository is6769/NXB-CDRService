package org.example.cdrservice.entitites;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Сущность Абонент, представляющая пользователя телекоммуникационных услуг.
 * <p>
 * Абонент идентифицируется по уникальному номеру телефона (MSISDN).
 * Эта сущность является основной для учета и генерации записей о звонках.
 * </p>
 *
 * @author Сервис роуминговой агрегации
 * @since 1.0
 */
@Entity
@Table(name = "subscribers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscriber {

    /**
     * Уникальный идентификатор абонента.
     * <p>
     * Автоматически генерируется базой данных при сохранении.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Номер мобильного телефона абонента в международном формате (MSISDN).
     * <p>
     * Это поле должно быть уникальным в системе и не может быть пустым.
     * Используется для идентификации абонента при обработке звонков.
     * </p>
     */
    @Column(name = "msisdn", unique = true, nullable = false)
    private String msisdn;

}
