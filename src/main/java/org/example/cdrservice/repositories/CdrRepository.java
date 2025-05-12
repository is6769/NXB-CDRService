package org.example.cdrservice.repositories;

import org.example.cdrservice.entitites.Cdr;
import org.example.cdrservice.entitites.ConsumedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий Spring Data JPA для сущностей {@link Cdr}.
 * Предоставляет CRUD-операции и пользовательские запросы для доступа к данным CDR.
 */
public interface CdrRepository extends JpaRepository<Cdr,Long> {

    /**
     * Извлекает ограниченное количество первых непотребленных CDR.
     * Непотребленные записи - это те, у которых {@code consumed_status} равен 'NEW'.
     * Записи обычно неявно упорядочены по времени их вставки или релевантной временной метке.
     *
     * @param limit Максимальное количество извлекаемых CDR.
     * @return Список первых {@code limit} непотребленных сущностей {@link Cdr}.
     */
    @Query(value = "select * from cdrs where consumed_status='NEW' limit :limit",nativeQuery = true)
    List<Cdr> findFirstNonConsumedRecords(@Param("limit") int limit);


    /**
     * Подсчитывает общее количество непотребленных CDR.
     * Непотребленные записи - это те, у которых {@code consumed_status} равен 'NEW'.
     *
     * @return Общее количество непотребленных CDR в виде {@link Integer}.
     */
    @Query(value = "select COUNT(*) from cdrs where consumed_status='NEW'",nativeQuery = true)
    Integer findNumberOfNonConsumedRows();
    //@Query("select * from Cdr c")
    //void findFirst10SortedWithDateTime();
}
