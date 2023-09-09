package com.thoainguyen.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "VerificationServiceClient", url = "${key-service.url}", fallback = VerificationServiceClientFallback.class)
public interface VerificationServiceClient {
  @PostMapping(value = "/kyc/{userName}", produces = MediaType.APPLICATION_JSON_VALUE)
  String verify(@PathVariable String userName);
}
