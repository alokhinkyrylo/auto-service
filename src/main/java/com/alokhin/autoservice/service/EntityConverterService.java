package com.alokhin.autoservice.service;

import com.alokhin.autoservice.persistence.model.entity.AccountEntity;
import com.alokhin.autoservice.persistence.model.entity.CarEntity;
import com.alokhin.autoservice.web.dto.AccountDto;
import com.alokhin.autoservice.web.dto.CarDto;

public interface EntityConverterService {

    AccountEntity toEntity(AccountDto dto);

    AccountDto toDto(AccountEntity entity);

    CarEntity toEntity(CarDto dto);

    CarDto toDto(CarEntity entity);
}
