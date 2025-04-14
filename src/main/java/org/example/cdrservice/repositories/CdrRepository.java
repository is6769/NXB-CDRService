package org.example.cdrservice.repositories;

import org.example.cdrservice.entitites.Cdr;
import org.example.cdrservice.entitites.ConsumedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

public interface CdrRepository extends JpaRepository<Cdr,Long> {
    List<Cdr> findCdrByConsumedStatus(ConsumedStatus consumedStatus);


    @Query(value = "select * from cdrs where consumed_status='NEW' limit 10",nativeQuery = true)
    List<Cdr> findFirst10NonConsumedRecords();


    @Query(value = "select COUNT(*) from cdrs where consumed_status='NEW'",nativeQuery = true)
    Integer findNumberOfNonConsumedRows();
    //@Query("select * from Cdr c")
    //void findFirst10SortedWithDateTime();
}
