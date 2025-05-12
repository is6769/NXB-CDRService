package org.example.cdrservice.dtos;

import org.example.cdrservice.entitites.Cdr;

import java.time.LocalDateTime;

/**
 * DTO для сущностей {@link Cdr}.
 * Используется для передачи информации о CDR, обычно внешним системам или слоям.
 *
 * @param callType Тип вызова (например, "01" для входящего, "02" для исходящего).
 * @param servicedMsisdn MSISDN обслуживаемого абонента.
 * @param otherMsisdn MSISDN другой стороны в вызове.
 * @param startDateTime Дата и время начала вызова.
 * @param finishDateTime Дата и время окончания вызова.
 */
public record CdrDTO (
        String callType,
        String servicedMsisdn,
        String otherMsisdn,
        LocalDateTime startDateTime,
        LocalDateTime finishDateTime
){
    /**
     * Создает экземпляр {@code CdrDTO} из сущности {@link Cdr}.
     *
     * @param cdr Сущность {@link Cdr} для преобразования.
     * @return Новый экземпляр {@code CdrDTO}, заполненный данными из сущности.
     */
    public static CdrDTO createFromEntity(Cdr cdr){
        return new CdrDTO(
                cdr.getCallType(),
                cdr.getServicedMsisdn(),
                cdr.getOtherMsisdn(),
                cdr.getStartDateTime(),
                cdr.getFinishDateTime()
        );
    }
}
