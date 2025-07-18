import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.request;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PatientIntegrationTest {

  @BeforeAll
  public static void setup() {
    RestAssured.baseURI = "http://localhost:4004";
  }

  @Test
  public void shouldReturnPatientWithValidToken() {
    String token = getToken();

    given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/patients")
            .then()
            .statusCode(200)
            .body("patients", notNullValue());
  }

  @Test
  public void shouldReturn429AfterLimitExceeded() throws InterruptedException {
    String token = getToken();
    int total = 10;
    int tooManyRequests = 0;
    for (int i = 1; i <= total; i++) {
      Response response = RestAssured
              .given()
              .header("Authorization", "Bearer " + token)
              .get("/api/patients");
      System.out.printf("Request %d Status: %d%n", i, response.statusCode());

      if (response.statusCode() == 429) {
        tooManyRequests++;
      }
      Thread.sleep(50);
    }

    assertTrue(tooManyRequests >= 1, "expected at Least 1 request too be rate limited 429");



  }

  private static String getToken() {
    String loginPayload = """
              {
                "email": "testuser@test.com",
                "password": "password123"
              }
            """;
    String token = given()
            .contentType("application/json")
            .body(loginPayload)
            .when()
            .post("/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .get("token");
    return token;
  }
}
