package org.example.cdrservice.repositories;

import org.example.cdrservice.entitites.Subscriber;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    @Override
    <S extends Subscriber> List<S> findAll(Example<S> example);

}
