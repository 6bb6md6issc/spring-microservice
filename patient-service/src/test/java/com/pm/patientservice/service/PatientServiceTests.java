package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class PatientServiceTests {

  private PatientRepository patientRepository;
  private PatientService patientService;

  @BeforeEach
  public void setUp() {
    patientRepository = mock(PatientRepository.class);
    patientService = new PatientService(patientRepository);
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
}
