package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PatientServiceTests {

  private PatientRepository patientRepository;
  private PatientService patientService;
  private PatientRequestDTO validRequest;
  private UUID patientId;

  @BeforeEach
  public void setUp() {
    patientId = UUID.randomUUID();
    patientRepository = mock(PatientRepository.class);
    patientService = new PatientService(patientRepository, new BillingServiceGrpcClient("dummyAdress", 1234));
    validRequest = new PatientRequestDTO();
    validRequest.setName("John Smith");
    validRequest.setEmail("john@email.com");
    validRequest.setAddress("123 Main Street");
    validRequest.setDateOfBirth("2000-01-01");
    validRequest.setRegisteredDate("2023-01-01");
  }

  @Test
  public void testGetPatients_ReturnsMappedDTOs() {

    // Arrange
    Patient patient1 = new Patient();
    patient1.setId(UUID.randomUUID());
    patient1.setName("Alice");
    patient1.setEmail("alice@example.com");
    patient1.setAddress("123 Main St");
    patient1.setDateOfBirth(LocalDate.of(1990, 1, 1));

    Patient patient2 = new Patient();
    patient2.setId(UUID.randomUUID());
    patient2.setName("Bob");
    patient2.setEmail("bob@example.com");
    patient2.setAddress("456 Oak St");
    patient2.setDateOfBirth(LocalDate.of(1985, 5, 20));

    when(patientRepository.findAll()).thenReturn(List.of(patient1, patient2));

    // Act
    List<PatientResponseDTO> result = patientService.getPatients();

    // Assert
    assertEquals(2, result.size());

    PatientResponseDTO dto1 = result.get(0);
    assertEquals("Alice", dto1.getName());
    assertEquals("alice@example.com", dto1.getEmail());

    PatientResponseDTO dto2 = result.get(1);
    assertEquals("Bob", dto2.getName());
    assertEquals("bob@example.com", dto2.getEmail());

    verify(patientRepository, times(1)).findAll();
  }

  @Test
  public void testCreatePatient_Success() {
    // Arrange
    when(patientRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);

    Patient patient = new Patient();
    patient.setId(UUID.randomUUID());
    patient.setName("John Smith");
    patient.setEmail("john@email.com");
    patient.setAddress("123 Main Street");
    patient.setDateOfBirth(LocalDate.parse("2000-01-01"));
    patient.setRegisteredDate(LocalDate.parse("2023-01-01"));
    when(patientRepository.save(any(Patient.class))).thenReturn(patient);

    PatientResponseDTO responseDTO = new PatientResponseDTO();
    responseDTO.setName("John Smith");
    responseDTO.setEmail("john@email.com");
    responseDTO.setAddress("123 Main Street");
    responseDTO.setDateOfBirth("2000-01-01");

    // Act
    try (MockedStatic<PatientMapper> mockedStatic = mockStatic(PatientMapper.class)) {
      mockedStatic.when(() -> PatientMapper.toModel(any(PatientRequestDTO.class))).thenReturn(patient);
      mockedStatic.when(() -> PatientMapper.toDto(any(Patient.class))).thenReturn(responseDTO);

      PatientResponseDTO result = patientService.createPatient(validRequest);

      assertNotNull(result);
      verify(patientRepository).save(any(Patient.class));
      assertEquals(responseDTO.getName(), result.getName());
      assertEquals(responseDTO.getEmail(), result.getEmail());
      assertEquals(responseDTO.getAddress(), result.getAddress());
    }
  }

  @Test
  public void testCreatePatient_EmailAlreadyExists_ThrowsException() {
    // Arrange
    when(patientRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

    // Act Assert
    EmailAlreadyExistsException exception = assertThrows(
            EmailAlreadyExistsException.class,
            () -> patientService.createPatient(validRequest)
    );

    assertTrue(exception.getMessage().contains(validRequest.getEmail()));
    verify(patientRepository, never()).save(any(Patient.class));
  }

  @Test
  public void updatePatient_ShouldReturnUpdatedPatient_WhenInputIsValid() {

    Patient patient = new Patient();
    patient.setId(patientId);
    patient.setName(validRequest.getName());
    patient.setEmail(validRequest.getEmail());
    patient.setAddress(validRequest.getAddress());
    patient.setDateOfBirth(LocalDate.parse(validRequest.getDateOfBirth()));

    PatientResponseDTO responseDTO = new PatientResponseDTO();
    responseDTO.setName(validRequest.getName());
    responseDTO.setId(patientId.toString());
    responseDTO.setEmail(validRequest.getEmail());
    responseDTO.setAddress(validRequest.getAddress());
    responseDTO.setDateOfBirth(validRequest.getDateOfBirth());

    when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
    when(patientRepository.existsByEmailAndIdNot(validRequest.getEmail(), patientId)).thenReturn(false);
    when(patientRepository.save(any(Patient.class))).thenReturn(patient);


    // Act and Assert
    try (MockedStatic<PatientMapper> mockedStatic = mockStatic(PatientMapper.class)) {
      mockedStatic.when(() -> PatientMapper.toDto(any(Patient.class))).thenReturn(responseDTO);

      PatientResponseDTO result = patientService.updatePatient(patientId, validRequest);

      assertEquals(patient.getName(), result.getName());
      assertEquals(patient.getAddress(), result.getAddress());
      assertEquals(patient.getEmail(), result.getEmail());
      assertEquals(patient.getDateOfBirth().toString(), result.getDateOfBirth());
      assertEquals(patient.getId().toString(), result.getId());

      verify(patientRepository).save(patient);
    }
  }

  @Test
  public void updatePatient_ShouldReturnUpdatedPatient_WhenIdDoesNotExist() {
    when(patientRepository.findById(patientId))
            .thenThrow(new PatientNotFoundException("Patient not find with ID: " + patientId));

    // Act and Assert
    PatientNotFoundException exception = assertThrows(
            PatientNotFoundException.class,
            () -> patientService.updatePatient(patientId, validRequest)
    );

    assertEquals("Patient not find with ID: " + patientId, exception.getMessage());
  }

  @Test
  public void updatePatient_ShouldReturnUpdatedPatient_WhenEmailIsinvalid() {
    Patient patient = new Patient();
    patient.setId(patientId);
    patient.setName(validRequest.getName());
    patient.setEmail(validRequest.getEmail());
    patient.setAddress(validRequest.getAddress());
    patient.setDateOfBirth(LocalDate.parse(validRequest.getDateOfBirth()));

    when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
    when(patientRepository.existsByEmailAndIdNot(eq(patient.getEmail()), any(UUID.class))).thenReturn(true);
    EmailAlreadyExistsException ex = assertThrows(
            EmailAlreadyExistsException.class,
            () -> patientService.updatePatient(patientId, validRequest)
    );

    assertEquals("A patient with this email already exists " + validRequest.getEmail(), ex.getMessage());
  }
}
