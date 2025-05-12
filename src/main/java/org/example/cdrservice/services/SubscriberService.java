package org.example.cdrservice.services;

import org.example.cdrservice.entitites.Subscriber;
import org.example.cdrservice.repositories.SubscriberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервисный слой для управления сущностями {@link Subscriber}.
 * Предоставляет методы для доступа к данным абонентов из репозитория.
 */
@Service
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;

    public SubscriberService(SubscriberRepository subscriberRepository) {
        this.subscriberRepository = subscriberRepository;
    }

    public List<Subscriber> findAll(){
        return subscriberRepository.findAll();
    }


}
