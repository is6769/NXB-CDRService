package org.example.cdrservice.services;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.cdrservice.entitites.Cdr;
import org.example.cdrservice.entitites.ConsumedStatus;
import org.example.cdrservice.entitites.Subscriber;
import org.example.cdrservice.repositories.CdrRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Сервис, отвечающий за генерацию CDR.
 */
@Service
@Slf4j
public class CdrProducerService {

    /**
     * Флаг, указывающий, завершена ли начальная генерация CDR и готовы ли данные к сохранению.
     */
    private boolean doReadyToPersist = false;

    @Value("${const.numberOfGenerationThreads}")
    private int numberOfGenerationThreads;

    /**
     * Приоритетная очередь для хранения сгенерированных CDR перед их сохранением.
     * CDR упорядочены по времени их завершения.
     */
    private PriorityBlockingQueue<Cdr> generatedCdrsQueue = new PriorityBlockingQueue<>(8192,Comparator.comparing(Cdr::getFinishDateTime));

    /**
     * Блокировка для обеспечения потокобезопасного доступа к {@code generatedCdrsQueue} и связанным операциям.
     */
    private final ReentrantLock lock = new ReentrantLock();

    private final CdrRepository cdrRepository;
    private final SubscriberService subscriberService;

    public CdrProducerService(CdrRepository cdrRepository, SubscriberService subscriberService) {
        this.cdrRepository = cdrRepository;
        this.subscriberService = subscriberService;
    }

    /**
     * Инициализирует процесс генерации CDR при запуске приложения.
     * Запускает несколько потоков для параллельной генерации CDR за последний год.
     * Устанавливает {@code doReadyToPersist} в true после завершения начальной генерации.
     */
    @PostConstruct
    public void runInitialGeneration(){
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfGenerationThreads; i++) {
            futures.add(CompletableFuture.runAsync(this::generateCdrForOneYear));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        doReadyToPersist = true;



    }

    /**
     * Генерирует случайные записи CDR за последний год.
     * Создает от 1000 до 2000 записей о звонках между абонентами, найденными в системе.
     * Каждая сгенерированная CDR затем обрабатывается и добавляется в набор данных.
     */
    public void generateCdrForOneYear(){

        List<Subscriber> subscribers = subscriberService.findAll();

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
            generatedCdr.setServicedMsisdn(caller.getMsisdn());
            generatedCdr.setOtherMsisdn(called.getMsisdn());
            generatedCdr.setStartDateTime(callStartDateTime);
            generatedCdr.setFinishDateTime(callFinishDateTime);
            generatedCdr.setConsumedStatus(ConsumedStatus.NEW);

            addToDataSet(generatedCdr);
        }
    }

    /**
     * Добавляет сгенерированную CDR в набор данных после необходимой обработки.
     * Этот метод гарантирует, что вызов разрешен для обоих участвующих абонентов,
     * разделяет CDR, если он пересекает полночь, создает зеркальную CDR, а затем добавляет
     * все результирующие CDR (оригинальные, разделенные части и зеркальные) в {@code generatedCdrsQueue}.
     * Эта операция потокобезопасна.
     *
     * @param cdr CDR для добавления в набор данных.
     */
    private void addToDataSet(Cdr cdr){
        lock.lock();
        try {
            if (isCallAllowed(cdr.getServicedMsisdn(), cdr.getStartDateTime(), cdr.getFinishDateTime())
                    && isCallAllowed(cdr.getOtherMsisdn(), cdr.getStartDateTime(), cdr.getFinishDateTime())) {
                List<Cdr> splittedCdrs = splitIfCrossesMidnight(cdr);
                List<Cdr> mirroredSplittedCdrs = makeMirrorCdrs(splittedCdrs);
                generatedCdrsQueue.addAll(splittedCdrs);
                generatedCdrsQueue.addAll(mirroredSplittedCdrs);
            }
        }finally {
            lock.unlock();
        }
    }

    /**
     * Создает зеркальные CDR для данного списка оригинальных CDR.
     * Зеркальная CDR представляет другую сторону вызова. Например, если оригинальная CDR
     * является исходящим вызовом для абонента А к Б, зеркальная CDR будет входящим вызовом
     * для абонента Б от А.
     *
     * @param originalCdrs Список оригинальных CDR.
     * @return Список зеркальных CDR.
     */
    private List<Cdr> makeMirrorCdrs(List<Cdr> originalCdrs){
        List<Cdr> mirrorCdrs = new ArrayList<>();
        originalCdrs.forEach(cdr -> {
            mirrorCdrs.add(Cdr.builder()
                            .callType((cdr.getCallType().equals("01")) ? "02" : "01") // Меняем тип вызова на противоположный
                            .servicedMsisdn(cdr.getOtherMsisdn()) // Обслуживаемый номер становится другим номером
                            .otherMsisdn(cdr.getServicedMsisdn()) // Другой номер становится обслуживаемым
                            .startDateTime(cdr.getStartDateTime())
                            .finishDateTime(cdr.getFinishDateTime())
                            .consumedStatus(cdr.getConsumedStatus())
                    .build());
        });
        return mirrorCdrs;
    }

    /**
     * Проверяет, разрешен ли вызов для данного номера телефона в указанном временном диапазоне.
     * Вызов не разрешен, если абонент уже участвует в другом вызове,
     * который пересекается с временным диапазоном нового вызова.
     * Эта проверка учитывает CDR, находящиеся в данный момент в {@code generatedCdrsQueue}.
     *
     * @param phoneNumber MSISDN абонента.
     * @param newStart Дата и время начала нового вызова.
     * @param newFinish Дата и время окончания нового вызова.
     * @return {@code true}, если вызов разрешен, {@code false} в противном случае.
     */
    private boolean isCallAllowed(String phoneNumber, LocalDateTime newStart, LocalDateTime newFinish){
        List<Cdr> allCdrsFinishedAfterStartOfNew = generatedCdrsQueue.stream().dropWhile(cdr -> newStart.isAfter(cdr.getFinishDateTime())).toList();

        for (Cdr existing: allCdrsFinishedAfterStartOfNew){
            if ((Objects.equals(existing.getServicedMsisdn(), phoneNumber) || Objects.equals(existing.getOtherMsisdn(), phoneNumber)) &&
                    !(newFinish.isBefore(existing.getStartDateTime()) || newStart.isAfter(existing.getFinishDateTime()))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Разделяет CDR на несколько CDR, если он пересекает одну или несколько полночей.
     * Каждая результирующая CDR будет содержаться в пределах одного дня.
     * Например, вызов с 23:00 Дня1 до 01:00 Дня2 будет разделен на
     * две CDR: одну с 23:00 до 23:59:59 Дня1, и другую с 00:00:00 до 01:00 Дня2.
     *
     * @param cdr CDR для разделения.
     * @return Список CDR, где каждая CDR не пересекает полночь. Если оригинальная CDR
     *         не пересекает полночь, список будет содержать только оригинальную CDR.
     */
    private List<Cdr> splitIfCrossesMidnight(Cdr cdr) {
        List<Cdr> result = new ArrayList<>();
        var currentStart = cdr.getStartDateTime();
        var currentEnd = cdr.getFinishDateTime();

        while (currentStart.isBefore(currentEnd)){
            LocalDateTime nextMidnight = currentStart.toLocalDate().plusDays(1).atStartOfDay();

            if (nextMidnight.isAfter(currentEnd)){
                result.add(new Cdr(
                        null,
                        cdr.getCallType(),
                        cdr.getServicedMsisdn(),
                        cdr.getOtherMsisdn(),
                        currentStart,
                        currentEnd,
                        cdr.getConsumedStatus()
                ));
                break;
            }

            result.add(new Cdr(
                    null,
                    cdr.getCallType(),
                    cdr.getServicedMsisdn(),
                    cdr.getOtherMsisdn(),
                    currentStart,
                    nextMidnight.minusSeconds(1),
                    cdr.getConsumedStatus()
            ));
            currentStart=nextMidnight;
        }
        return result;
    }

    /**
     * Периодически сохраняет пакет CDR из {@code generatedCdrsQueue} в базу данных.
     * Этот метод запланирован для запуска с фиксированной скоростью, определенной {@code const.scheduled.produce-cdr-rate}.
     * Он выполняется, только если {@code doReadyToPersist} равно true.
     * Извлекает случайное количество CDR (до 5) из очереди и сохраняет их.
     */
    @Async
    @Scheduled(fixedRateString = "${const.scheduled.produce-cdr-rate}")
    public void persistQueuedData(){
        if (!doReadyToPersist) return;
        var numberOfCdrs = ThreadLocalRandom.current().nextInt(5);
        List<Cdr> cdrsToPersist = new ArrayList<>();
        for (int i = 0; i <numberOfCdrs ; i++) {
            if (generatedCdrsQueue.isEmpty()) break;
            cdrsToPersist.add(generatedCdrsQueue.poll());
        }

        cdrRepository.saveAll(cdrsToPersist);


    }
}
