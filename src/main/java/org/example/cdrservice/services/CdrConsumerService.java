package org.example.cdrservice.services;

import lombok.extern.slf4j.Slf4j;
import org.example.cdrservice.dtos.CdrDTO;
import org.example.cdrservice.entitites.Cdr;
import org.example.cdrservice.entitites.ConsumedStatus;
import org.example.cdrservice.repositories.CdrRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class CdrConsumerService {

    @Value("${const.numberOfRecordsInCDR}")
    private int numberOfRecordsInCDR;

    private final CdrRepository cdrRepository;
    private final RabbitTemplate rabbitTemplate;

    public CdrConsumerService(CdrRepository cdrRepository, RabbitTemplate rabbitTemplate) {
        this.cdrRepository = cdrRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Async
    @Scheduled(fixedRate = 5000)
    public void consumeDataFromDB(){
        if (cdrRepository.findNumberOfNonConsumedRows()<10) return;
        List<Cdr> consumedCdrs = cdrRepository.findFirst10NonConsumedRecords();

        List<CdrDTO> dtos = consumedCdrs.stream().map(CdrDTO::createFromEntity).toList();
        rabbitTemplate.convertAndSend("cdr.direct","cdr.created",dtos);

        consumedCdrs.forEach(cdr -> {
            log.info(String.valueOf(cdr));
            cdr.setConsumedStatus(ConsumedStatus.CONSUMED);
            cdrRepository.save(cdr);
        });




    }
}
