package com.thoainguyen.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.metric.consumer.HealthCountsStream;
import com.thoainguyen.Application;
import com.thoainguyen.util.ApplicationUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@EnableConfigurationProperties
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { Application.class })
@AutoConfigureWireMock(port = 8082)
public class VerificationServiceClientTest {

  @Autowired
  private VerificationServiceClient verificationServiceClient;

  @BeforeEach
  public void before() {
    Hystrix.reset();
    HealthCountsStream.reset();
  }

  @Test
  @DisplayName("fallback will be triggered when the response is error")
  public void fallBackWillBeTriggered_after_responseIsError() throws InterruptedException {
    WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/kyc/thoai"))
      .willReturn(WireMock.aResponse()
        .withStatus(500)));
    String response = verificationServiceClient.verify("thoai");
    Assertions.assertEquals(response, ApplicationUtil.FALLBACK_MESSAGE);
  }

  @Test
  @DisplayName("fallback will not be triggered when the response is success")
  public void fallBackWillNotBeTriggered_after_responseIsSuccess() throws InterruptedException {
    WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/kyc/thoai"))
      .willReturn(WireMock.aResponse()
        .withBody(ApplicationUtil.SUCCESS_MESSAGE)
        .withStatus(200)));
    String response = verificationServiceClient.verify("thoai");
    Assertions.assertEquals(response, ApplicationUtil.SUCCESS_MESSAGE);
  }

  @Test
  @DisplayName("fallback will be triggered when the response is timeout")
  public void fallBackWillBeTriggered_after_requestTimeOut() throws InterruptedException {
    /**
     * Given: hystrix circuit breaker is configured with timeoutInMilliseconds = 3000ms,
     */

    /**
     * When: the request is response after 4000ms
     */
    WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/kyc/thoai"))
      .willReturn(WireMock.aResponse()
        .withFixedDelay(4000)
        .withStatus(200)));
    String response = verificationServiceClient.verify("thoai");

    /**
     * Then: fallback will be triggered
     */
    Assertions.assertEquals(response, ApplicationUtil.FALLBACK_MESSAGE);
  }

  @Test
  @DisplayName("fallback will not be triggered when the response is slow but is still less than timeoutInMilliseconds")
  public void fallBackWillNotBeTriggered_when_requestNotTimeOut() throws InterruptedException {
    /**
     * Given: hystrix circuit breaker is configured with timeoutInMilliseconds = 3000ms,
     */

    /**
     * When: the request is response after 2000ms
     */
    WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/kyc/thoai"))
      .willReturn(WireMock.aResponse()
        .withFixedDelay(2000)
        .withBody(ApplicationUtil.SUCCESS_MESSAGE)
        .withStatus(200)));
    String response = verificationServiceClient.verify("thoai");

    /**
     * Then: fallback will not be triggered
     */
    Assertions.assertEquals(response, ApplicationUtil.SUCCESS_MESSAGE);
  }

  @Test
  @DisplayName("the circuit is open when total execute time of all requests is over sleepWindowInMilliseconds configure AND "
    + "the error rate is over errorThresholdPercentage configure AND "
    + "the total requests is over requestVolumeThreshold configure")
  public void circuitWillBeOpened() throws InterruptedException {
    /**
     * Given: hystrix circuit breaker is configured with requestVolumeThreshold = 5,
     * sleepWindowInMilliseconds: 2000
     * errorThresholdPercentage: 40 %
     */

    /**
     * When: 3/5 requests is failing (over 40%)
     */
    WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/kyc/thoai"))
      .willReturn(WireMock.aResponse()
        .withStatus(200)));

    WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/kyc/camila"))
      .willReturn(WireMock.aResponse()
        .withStatus(500)));

    verificationServiceClient.verify("thoai");
    verificationServiceClient.verify("camila");
    verificationServiceClient.verify("camila");
    verificationServiceClient.verify("thoai");
    verificationServiceClient.verify("camila");

    /**
     * Then: after 3000ms, the circuit should be opened
     */
    Thread.sleep(3000);

    HystrixCircuitBreaker myCircuitBreaker = HystrixCircuitBreaker.Factory.getInstance(
      HystrixCommandKey.Factory.asKey("VerificationServiceClient#verify(String)"));

    Assertions.assertTrue(myCircuitBreaker.isOpen());
  }

  @Test
  @DisplayName("the circuit is still close when total execute time of all requests is less then sleepWindowInMilliseconds configure")
  public void circuitWillNotBeOpened_1() throws InterruptedException {
    /**
     * Given: hystrix circuit breaker is configured with requestVolumeThreshold = 5,
     * sleepWindowInMilliseconds: 2000
     * errorThresholdPercentage: 40 %
     */

    /**
     * When: 3/5 requests is failing (over 40%)
     */
    WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/kyc/thoai"))
      .willReturn(WireMock.aResponse()
        .withStatus(200)));

    WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/kyc/camila"))
      .willReturn(WireMock.aResponse()
        .withStatus(500)));

    verificationServiceClient.verify("thoai");
    verificationServiceClient.verify("camila");
    verificationServiceClient.verify("camila");
    verificationServiceClient.verify("thoai");
    verificationServiceClient.verify("camila");

    /**
     * Then: the circuit is close since sleepWindowInMilliseconds = 2000ms
     */
    HystrixCircuitBreaker myCircuitBreaker = HystrixCircuitBreaker.Factory.getInstance(
      HystrixCommandKey.Factory.asKey("VerificationServiceClient#verify(String)"));

    Assertions.assertTrue(!myCircuitBreaker.isOpen());
  }

  @Test
  @DisplayName("the circuit is still close when the error rate is less than errorThresholdPercentage configure")
  public void circuitWillNotBeOpened_2() throws InterruptedException {
    /**
     * Given: hystrix circuit breaker is configured with requestVolumeThreshold = 5,
     * sleepWindowInMilliseconds: 2000
     * errorThresholdPercentage: 40 %
     */

    /**
     * When: 1/5 requests is failing (less than 40%)
     */
    WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/kyc/thoai"))
      .willReturn(WireMock.aResponse()
        .withStatus(200)));

    WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/kyc/camila"))
      .willReturn(WireMock.aResponse()
        .withStatus(500)));

    verificationServiceClient.verify("thoai");
    verificationServiceClient.verify("camila");
    verificationServiceClient.verify("thoai");
    verificationServiceClient.verify("thoai");
    verificationServiceClient.verify("thoai");

    /**
     * Then: after 3000ms, the circuit is still close since errorThresholdPercentage less than 40%
     */
    Thread.sleep(3000);

    HystrixCircuitBreaker myCircuitBreaker = HystrixCircuitBreaker.Factory.getInstance(
      HystrixCommandKey.Factory.asKey("VerificationServiceClient#verify(String)"));

    Assertions.assertTrue(!myCircuitBreaker.isOpen());
  }

  @Test
  @DisplayName("the circuit is still close when the total requests is less than requestVolumeThreshold configure")
  public void circuitWillNotBeOpened_3() throws InterruptedException {
    /**
     * Given: hystrix circuit breaker is configured with requestVolumeThreshold = 5,
     * sleepWindowInMilliseconds: 2000
     * errorThresholdPercentage: 40 %
     */

    /**
     * When: 2/2 requests is failing (over 40%)
     */
    WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/kyc/camila"))
      .willReturn(WireMock.aResponse()
        .withStatus(500)));

    verificationServiceClient.verify("camila");
    verificationServiceClient.verify("camila");

    /**
     * Then: after 3000ms, the circuit is still close since total requests is less than requestVolumeThreshold
     */
    Thread.sleep(3000);

    HystrixCircuitBreaker myCircuitBreaker = HystrixCircuitBreaker.Factory.getInstance(
      HystrixCommandKey.Factory.asKey("VerificationServiceClient#verify(String)"));

    Assertions.assertTrue(!myCircuitBreaker.isOpen());
  }
}
