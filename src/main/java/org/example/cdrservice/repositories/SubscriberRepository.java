package org.example.cdrservice.repositories;

import org.example.cdrservice.entitites.Subscriber;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с сущностью Абонент.
 * <p>
 * Обеспечивает доступ к базе данных для выполнения операций
 * создания, чтения, обновления и удаления (CRUD) абонентов.
 * Также предоставляет дополнительные методы поиска абонентов.
 * </p>
 *
 * @author Сервис роуминговой агрегации
 * @since 1.0
 */
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    /**
     * Поиск всех абонентов, соответствующих примеру.
     *
     * @param example Пример абонента для поиска соответствий
     * @param <S> Тип, производный от сущности Subscriber
     * @return Список абонентов, соответствующих примеру
     */
    @Override
    <S extends Subscriber> List<S> findAll(Example<S> example);

    /**
     * Поиск абонента по номеру телефона (MSISDN).
     * <p>
     * Метод ищет абонента по точному совпадению номера телефона.
     * </p>
     *
     * @param msisdn Номер мобильного телефона абонента
     * @return Optional, содержащий найденного абонента, или пустой Optional, если абонент не найден
     */
    Optional<Subscriber> findSubscriberByMsisdn(String msisdn);
}
