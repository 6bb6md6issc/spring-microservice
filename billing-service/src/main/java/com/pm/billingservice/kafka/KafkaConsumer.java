package com.pm.billingservice.kafka;

import billing.events.BillingAccountEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class KafkaConsumer {

  private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

  @KafkaListener(topics = "billing-account", groupId = "billing-service")
  public void consumeEvent(byte[] event) {
    try{
      BillingAccountEvent billingAccountEvent = BillingAccountEvent.parseFrom(event);
      log.info(
              "Received billing account event: [PatientId={}, PatientName={}, PatientEmail={}]",
              billingAccountEvent.getPatientId(),
              billingAccountEvent.getName(),
              billingAccountEvent.getEmail()
              );
      // check if the patient billing account doesn't exist, if not then create it

    }catch (InvalidProtocolBufferException e){
      log.error("Error parsing BillingAccountEvent: {}", e.getMessage());
    }
  }
}
