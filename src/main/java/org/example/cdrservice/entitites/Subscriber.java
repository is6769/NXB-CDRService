package org.example.cdrservice.entitites;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Сущность, представляющая абонента телекоммуникационных услуг.
 * <p>
 * Каждый абонент уникально идентифицируется своим MSISDN (номером телефона).
 * Эта сущность используется для связывания записей данных о вызовах с конкретными пользователями.
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
     * Международный идентификатор мобильного абонента (MSISDN).
     * <p>
     * Это номер телефона абонента, который должен быть уникальным и не может быть пустым.
     * Используется в качестве основного идентификатора абонентов при обработке вызовов.
     * </p>
     */
    @Column(name = "msisdn", unique = true, nullable = false)
    private String msisdn;

}
