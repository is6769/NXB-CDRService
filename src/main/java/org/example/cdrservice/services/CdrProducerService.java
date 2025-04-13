package org.example.cdrservice.services;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.cdrservice.entitites.Cdr;
import org.example.cdrservice.entitites.ConsumedStatus;
import org.example.cdrservice.entitites.Subscriber;
import org.example.cdrservice.repositories.CdrRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class CdrProducerService {

    private boolean doReadyToPersist = false;

    @Value("${const.numberOfGenerationThreads}")
    private int numberOfGenerationThreads;

    private final PriorityBlockingQueue<Cdr> generatedCdrsQueue = new PriorityBlockingQueue<>(5*1000,Comparator.comparing(Cdr::getFinishDateTime));

    private final CdrRepository cdrRepository;
    private final SubscriberService subscriberService;

    public CdrProducerService(CdrRepository cdrRepository, SubscriberService subscriberService) {
        this.cdrRepository = cdrRepository;
        this.subscriberService = subscriberService;
    }

    @PostConstruct
    public void runInitialGeneration(){
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfGenerationThreads; i++) {
            futures.add(CompletableFuture.runAsync(this::generateCdrForOneYear));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("DATA GENERATED");

        doReadyToPersist = true;



    }

    /**
     * Генерирует случайные записи CDR за последний год.
     * Создает от 1000 до 2000 записей о звонках между абонентами.
     */
    public void generateCdrForOneYear(){

        log.info("STARTED DATA GENERATION IN THREAD: {}", Thread.currentThread().getName());

        List<Subscriber> subscribers = subscriberService.findAll();

        List<Cdr> generatedCdrs = new ArrayList<>();

        LocalDateTime startDateTime = LocalDateTime.now().minusYears(1);
        LocalDateTime endDateTime = LocalDateTime.now();

        long startMillis = startDateTime.atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli();
        long endMillis = endDateTime.atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli();

        int totalNumberOfCalls = ThreadLocalRandom.current().nextInt(1000,2001);

        for (int i = 0; i < totalNumberOfCalls; i++) {

            Cdr generatedCdr = new Cdr();


            String callType = (ThreadLocalRandom.current().nextBoolean()) ? "01" : "02";


            int randomCallerIndex = ThreadLocalRandom.current().nextInt(subscribers.size());

            int randomCalledIndex;
            do {
                randomCalledIndex = ThreadLocalRandom.current().nextInt(subscribers.size());
            }while (randomCalledIndex == randomCallerIndex);

            Subscriber caller = subscribers.get(randomCallerIndex);
            Subscriber called = subscribers.get(randomCalledIndex);


            long durationMillis = ThreadLocalRandom.current().nextLong(1,5*60*60*1000);

            long callStartMillis = ThreadLocalRandom.current().nextLong(startMillis,endMillis-durationMillis);//endMillis-durationMillis to make [l;r) maybe redo
            long callFinishMillis = callStartMillis + durationMillis;

            var callStartDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(callStartMillis),ZoneId.of("Europe/Moscow"));
            var callFinishDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(callFinishMillis),ZoneId.of("Europe/Moscow"));

            generatedCdr.setCallType(callType);
            generatedCdr.setCallerNumber(caller.getMsisdn());
            generatedCdr.setCalledNumber(called.getMsisdn());
            generatedCdr.setStartDateTime(callStartDateTime);
            generatedCdr.setFinishDateTime(callFinishDateTime);
            generatedCdr.setConsumedStatus(ConsumedStatus.NEW);

//            List<Cdr> cdrAfterSplit = tryToSplitCdr(generatedCdr);
//            if (Objects.nonNull(cdrAfterSplit)){
//                generatedCdrs.addAll(cdrAfterSplit);
//            }

            generatedCdrs.add(generatedCdr);
        }

        generatedCdrs.sort(Comparator.comparing(Cdr::getFinishDateTime));

        generatedCdrsQueue.addAll(generatedCdrs);
    }

//    private List<Cdr> tryToSplitCdr(Cdr generatedCdr) {
//        var start = generatedCdr.getStartDateTime();
//        var finish = generatedCdr.getFinishDateTime();
//        long daysBetween = ChronoUnit.DAYS.between(start.toLocalDate(), finish.toLocalDate());
//
//        // If same day, no midnights crossed
//        if (daysBetween == 0) return null;
//
//        // Perform action for each midnight crossing
//        for (int i = 1; i <= daysBetween; i++) {
//            LocalDateTime midnight = start.toLocalDate().plusDays(i).atStartOfDay();
//            performActionAtMidnight(midnight);
//        }
//        if (generatedCdr.getStartDateTime().isBefore(LocalTime.MIDNIGHT) && generatedCdr.getFinishDateTime().isAfter(LocalTime.MIDNIGHT)){
//
//        }
//    }

    @Async
    @Scheduled(fixedRate = 5000)
    public void persistQueuedData(){
        if (!doReadyToPersist) return;
        var numberOfCdrs = ThreadLocalRandom.current().nextInt(5);
        List<Cdr> cdrsToPersist = new ArrayList<>();
        for (int i = 0; i <numberOfCdrs ; i++) {
            cdrsToPersist.add(generatedCdrsQueue.poll());
        }

        cdrRepository.saveAll(cdrsToPersist);


    }


}
