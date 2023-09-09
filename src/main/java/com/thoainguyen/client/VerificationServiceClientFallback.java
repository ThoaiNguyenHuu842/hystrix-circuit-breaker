package com.thoainguyen.client;

import com.thoainguyen.util.ApplicationUtil;
import org.springframework.stereotype.Component;

@Component
public class VerificationServiceClientFallback implements VerificationServiceClient{

  @Override
  public String verify(String userName) {
    return ApplicationUtil.FALLBACK_MESSAGE;
  }
}
