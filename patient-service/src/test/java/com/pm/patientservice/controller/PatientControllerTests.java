package com.pm.patientservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
public class PatientControllerTests {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private PatientService patientService;

  @Autowired
  private ObjectMapper objectMapper;

  private PatientResponseDTO dto1;
  private PatientResponseDTO dto2;

  @BeforeEach
  public void setup() {
    dto1 = new PatientResponseDTO();
    dto1.setAddress("123 Main St");
    dto1.setName("Alice");
    dto1.setEmail("alice@example.com");
    dto1.setDateOfBirth(LocalDate.of(1985, 5, 20).toString());

    dto2 = new PatientResponseDTO();
    dto2.setName("Bob");
    dto2.setEmail("bob@example.com");
    dto2.setAddress("456 Oak St");
    dto2.setDateOfBirth(LocalDate.of(1985, 5, 20).toString());
  }

  @Test
  public void PatientController_GetPatients_ReturnsPatientList() throws Exception {
    // Arrange
    List<PatientResponseDTO> mockList = List.of(dto1, dto2);

    when(patientService.getPatients()).thenReturn(mockList);

    // Act + Assert
    mockMvc.perform(get("/patients"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Alice"))
            .andExpect(jsonPath("$[1].email").value("bob@example.com"));
  }

  @Test
  public void patient_controller_createPatient_returnResponseEntityOk() throws Exception {
    // Arrange
    PatientRequestDTO requestDTO = new PatientRequestDTO();
    requestDTO.setName("John Smith");
    requestDTO.setEmail("john@email.com");
    requestDTO.setAddress("123 Main Street");
    requestDTO.setDateOfBirth("2000-01-01");
    requestDTO.setRegisteredDate("2023-01-01");

    PatientResponseDTO responseDTO = new PatientResponseDTO();
    responseDTO.setName("John Smith");
    responseDTO.setEmail("john@email.com");
    responseDTO.setAddress("123 Main Street");
    responseDTO.setDateOfBirth("2000-01-01");

    when(patientService.createPatient(any(PatientRequestDTO.class))).thenReturn(responseDTO);

    // Act Assert
    mockMvc.perform(post("/patients")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("John Smith"))
            .andExpect(jsonPath("$.email").value("john@email.com"))
            .andExpect(jsonPath("$.address").value("123 Main Street"))
            .andExpect(jsonPath("$.dateOfBirth").value("2000-01-01"));
  }

  @Test
  public void patientController_updatePatient_returnResponseEntityOk() throws Exception {
    // Arrange
    UUID patientId = UUID.randomUUID();

    PatientRequestDTO requestDTO = new PatientRequestDTO();
    requestDTO.setName("John Smith");
    requestDTO.setAddress("123 Main Street");
    requestDTO.setEmail("example@email.com");
    requestDTO.setDateOfBirth("2000-01-01");

    PatientResponseDTO responseDTO = new PatientResponseDTO();
    responseDTO.setId(patientId.toString());
    responseDTO.setName("John Smith");
    responseDTO.setDateOfBirth("2000-01-01");
    responseDTO.setEmail("example@email.com");
    responseDTO.setAddress("123 Main Street");
    when(patientService.updatePatient(eq(patientId), any(PatientRequestDTO.class)))
            .thenReturn(responseDTO);

    // Act
    mockMvc.perform(put("/patients/" + patientId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("John Smith"))
            .andExpect(jsonPath("$.email").value("example@email.com"))
            .andExpect(jsonPath("$.address").value("123 Main Street"))
            .andExpect(jsonPath("$.dateOfBirth").value("2000-01-01"));
  }
}
