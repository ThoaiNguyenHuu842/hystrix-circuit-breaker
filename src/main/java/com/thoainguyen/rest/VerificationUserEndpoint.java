package com.thoainguyen.rest;

import com.thoainguyen.service.VerificationUserService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class VerificationUserEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(VerificationUserEndpoint.class);
  private final VerificationUserService verificationUserService;

  @PostMapping("/verify/{userName}")
  public ResponseEntity<String> verify(@PathVariable String userName) {
    return ResponseEntity.ok(verificationUserService.verify(userName));
  }
}
