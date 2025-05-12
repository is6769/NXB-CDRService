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

/**
 * Сервис, отвечающий за потребление CDR из базы данных
 * и отправку их в очередь сообщений (RabbitMQ).
 * Периодически проверяет наличие новых CDR, обрабатывает их пакетами
 * и помечает как потребленные.
 */
@Slf4j
@Service
public class CdrConsumerService {

    /**
     * Количество записей CDR для извлечения и обработки в одном пакете.
     * Настраивается через свойство {@code const.numberOfRecordsInCDR}.
     */
    @Value("${const.numberOfRecordsInCDR}")
    private int numberOfRecordsInCDR;

    /**
     * Имя обменника RabbitMQ, в который будут отправляться CDR.
     */
    @Value("${const.rabbitmq.cdr.CDR_EXCHANGE_NAME}")
    private String CDR_EXCHANGE_NAME;

    /**
     * Ключ маршрутизации RabbitMQ, используемый при отправке CDR.
     */
    @Value("${const.rabbitmq.cdr.CDR_ROUTING_KEY}")
    private String CDR_ROUTING_KEY;


    private final CdrRepository cdrRepository;
    private final RabbitTemplate rabbitTemplate;

    public CdrConsumerService(CdrRepository cdrRepository, RabbitTemplate rabbitTemplate) {
        this.cdrRepository = cdrRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Периодически потребляет данные CDR из базы данных и отправляет их в RabbitMQ.
     * Этот метод запланирован для запуска с фиксированной скоростью, определенной {@code const.scheduled.consume-cdr-rate}.
     * <p>
     * Проверяет, достаточно ли непотребленных CDR (по крайней мере, {@code numberOfRecordsInCDR}).
     * Если да, извлекает пакет CDR, преобразует их в DTO, отправляет в RabbitMQ,
     * а затем обновляет их статус на {@link ConsumedStatus#CONSUMED} в базе данных.
     * </p>
     */
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
