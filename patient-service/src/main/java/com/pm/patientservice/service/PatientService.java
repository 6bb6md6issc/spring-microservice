package com.pm.patientservice.service;

import com.pm.patientservice.dto.PagedPatientResponseDto;
import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.kafkaProducer;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {
  private static final Logger log = LoggerFactory.getLogger(PatientService.class);
  private final PatientRepository patientRepository;
  private final BillingServiceGrpcClient billingServiceGrpcClient;
  private final kafkaProducer kafkaProducer;

  public PatientService(
          PatientRepository patientRepository,
          BillingServiceGrpcClient billingServiceGrpcClient,
          kafkaProducer kafkaProducer
  ) {
    this.patientRepository = patientRepository;
    this.billingServiceGrpcClient = billingServiceGrpcClient;
    this.kafkaProducer = kafkaProducer;
  }

  @Cacheable(
          value = "patients",
          key = "#page + '-' + #size + '-' +  #sort + '-' + #sortField",
          condition = "#searchValue == ''"
  )
  public PagedPatientResponseDto getPatients (
          int page,
          int size,
          String sort,
          String sortField,
          String searchValue
  ) {
    log.info("[REDIS]: cache miss - fetching from db");
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      log.error(e.getMessage());
    }
    Pageable pageable = PageRequest.of(
            page-1,
            size,
            sort.equalsIgnoreCase("desc")
                    ? Sort.by(sortField).descending()
                    : Sort.by(sortField).ascending()
    );

    Page<Patient> patientPage;

    if (searchValue == null || searchValue.isBlank()) {
      patientPage = patientRepository.findAll(pageable);
    } else {
      patientPage = patientRepository.findByNameContainingIgnoreCase(searchValue, pageable);
    }

    List<PatientResponseDTO> patientResponseDtos = patientPage.getContent()
            .stream()
            .map(PatientMapper::toDto)
            .toList();

    return new PagedPatientResponseDto(
            patientResponseDtos,
            patientPage.getNumber() + 1,
            patientPage.getSize(),
            patientPage.getTotalPages(),
            (int) patientPage.getTotalElements()
    );
  }

  public PatientResponseDTO createPatient (PatientRequestDTO patientRequestDTO) {
    if (patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
      throw new EmailAlreadyExistsException("A patient with this email already exists" +
              patientRequestDTO.getEmail());
    }

    Patient newPatient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));

    billingServiceGrpcClient.createBillingAccount(
            newPatient.getId().toString(),
            newPatient.getName(),
            newPatient.getEmail()
    );

    kafkaProducer.sendEvent(newPatient);

    return PatientMapper.toDto(newPatient);
  }

  public PatientResponseDTO updatePatient (
          UUID id,
          PatientRequestDTO patientRequestDTO
  ) {
    Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new PatientNotFoundException("Patient not find with ID: " + id));
    if (patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), id)) {
      throw new EmailAlreadyExistsException(
              "A patient with this email already exists "
                      + patientRequestDTO.getEmail());
    }
    patient.setName(patientRequestDTO.getName());
    patient.setAddress(patientRequestDTO.getAddress());
    patient.setEmail(patientRequestDTO.getEmail());
    patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

    Patient updatedPatient = patientRepository.save(patient);
    return PatientMapper.toDto(updatedPatient);
  }

  public void deletePatient (UUID id) {
    patientRepository.deleteById(id);
  }
}
