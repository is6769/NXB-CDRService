package org.example.cdrservice.services;

import lombok.extern.slf4j.Slf4j;
import org.example.cdrservice.dtos.CdrDTO;
import org.example.cdrservice.entitites.Cdr;
import org.example.cdrservice.entitites.ConsumedStatus;
import org.example.cdrservice.repositories.CdrRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CdrConsumerService {

    @Value("${const.numberOfRecordsInCDR}")
    private int numberOfRecordsInCDR;

    @Value("${const.rabbitmq.cdr.CDR_EXCHANGE_NAME}")
    private String CDR_EXCHANGE_NAME;

    @Value("${const.rabbitmq.cdr.CDR_ROUTING_KEY}")
    private String CDR_ROUTING_KEY;


    private final CdrRepository cdrRepository;
    private final RabbitTemplate rabbitTemplate;

    public CdrConsumerService(CdrRepository cdrRepository, RabbitTemplate rabbitTemplate) {
        this.cdrRepository = cdrRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedRateString = "${const.scheduled.consume-cdr-rate}")
    public void consumeDataFromDB(){
        if (cdrRepository.findNumberOfNonConsumedRows()<numberOfRecordsInCDR) return;
        List<Cdr> consumedCdrs = cdrRepository.findFirstNonConsumedRecords(numberOfRecordsInCDR);

        List<CdrDTO> dtos = consumedCdrs.stream().map(CdrDTO::createFromEntity).toList();
        rabbitTemplate.convertAndSend(CDR_EXCHANGE_NAME,CDR_ROUTING_KEY,dtos);

        consumedCdrs.forEach(cdr -> {
            log.info(String.valueOf(cdr));
            cdr.setConsumedStatus(ConsumedStatus.CONSUMED);
            cdrRepository.save(cdr);
        });
    }
}
