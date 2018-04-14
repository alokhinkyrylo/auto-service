package com.alokhin.autoservice.web.controller;

import org.hibernate.validator.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.alokhin.autoservice.exception.AccountNotFoundException;
import com.alokhin.autoservice.exception.CarNotFoundException;
import com.alokhin.autoservice.persistence.model.entity.AccountEntity;
import com.alokhin.autoservice.persistence.model.entity.CarEntity;
import com.alokhin.autoservice.persistence.model.entity.RoleEntity;
import com.alokhin.autoservice.service.AccountService;
import com.alokhin.autoservice.service.CarService;
import com.alokhin.autoservice.service.DataService;
import com.alokhin.autoservice.service.EntityConverterService;
import com.alokhin.autoservice.web.dto.CarDto;
import com.alokhin.autoservice.web.dto.CreateCarDto;
import com.alokhin.autoservice.web.dto.ErrorDto;
import com.alokhin.autoservice.web.dto.MessageDto;

import java.util.List;
import java.util.stream.Collectors;

import static com.alokhin.autoservice.domain.ErrorResponse.PROCESSING_ERROR;

@Controller
@RequestMapping ("/api/car")
public class CarController {

    private static final Logger logger = LoggerFactory.getLogger(CarController.class);

    private final AccountService accountService;

    private final CarService carService;

    private final DataService dataService;

    private final EntityConverterService entityConverterService;

    @Autowired
    public CarController(CarService carService, DataService dataService, AccountService accountService, EntityConverterService entityConverterService) {
        this.carService = carService;
        this.dataService = dataService;
        this.accountService = accountService;
        this.entityConverterService = entityConverterService;
    }

    @PostMapping ("/add")
    @PreAuthorize ("hasAuthority('USER') or hasAuthority('ADMIN')")
    public ResponseEntity<?> addCar(@RequestBody CreateCarDto createCarDto) {
        try {
            AccountEntity accountEntity = accountService.findByLogin(getAccountFromContext());
            Boolean isAdmin = accountEntity.getRoles().stream().filter(RoleEntity::isAdmin).findFirst().isPresent();
            CarDto carDto = CarDto.builder().name(createCarDto.getName())
                                  .year(createCarDto.getYear())
                                  .price(createCarDto.getPrice())
                                  .login(accountEntity.getLogin())
                                  .description(createCarDto.getDescription())
                                  .enabled(isAdmin)
                                  .build();
            CarEntity carEntity = entityConverterService.toEntity(carDto);
            return ResponseEntity.ok(entityConverterService.toDto(carService.save(carEntity)));
        } catch (AccountNotFoundException a) {
            logger.error("Failed to add car. Account is not exists.", a);
            return new ResponseEntity<>(ErrorDto.builder().errorResponse(PROCESSING_ERROR).messageDto(new MessageDto(a.getMessage())).build(),
                                        HttpStatus.EXPECTATION_FAILED);
        }
    }

    @GetMapping (value = "/cars")
    public ResponseEntity<?> getCars(@RequestParam (required = false) Integer yearFrom, @RequestParam (required = false) Integer yearTo,
                                     @RequestParam (required = false) Integer priceFrom,
                                     @RequestParam (required = false) Integer priceTo) {
        List<CarEntity> cars = carService.findCars(yearFrom, yearTo, priceFrom, priceTo);
        return new ResponseEntity<>(cars.stream().map(entityConverterService::toDto).collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping (value = "/activate")
    public ResponseEntity<?> activateCar(@NotBlank @RequestParam Integer id) {
        try {
            CarEntity carEntity = carService.findById(id);
            carService.enableCar(carEntity);
        } catch (CarNotFoundException c) {
            logger.error("Failed to activate car advertisement with id={}, cause: ", id, c);
            return new ResponseEntity<>(ErrorDto.builder().errorResponse(PROCESSING_ERROR).messageDto(new MessageDto(c.getMessage())).build(),
                                        HttpStatus.EXPECTATION_FAILED);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private String getAccountFromContext() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}