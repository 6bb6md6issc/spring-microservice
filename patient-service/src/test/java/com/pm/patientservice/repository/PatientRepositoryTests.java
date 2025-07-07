package com.pm.patientservice.repository;

import com.pm.patientservice.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class PatientRepositoryTests {

  @Autowired
  private PatientRepository patientRepository;

  public void PatientRepositoryTests() {
    // Arrange

    // Act

    // Assert
  }

}
