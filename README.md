# Introduction

Hystrix is a library that controls the interaction between microservices to provide latency and fault tolerance. 
Hystrix implements Circuit breaker which is a pattern that wraps requests to external services and detects when they fail.
We will demonstrate how hystrix is applied in a microservices system via this project. 

This project is a home loaning service which consume a KYC service to verify customers by the username. You can
imagine these services are in a banking microservices-based system which can contain a lot of other services.
  
## Tech stack

JDK 11, Gradle, Spring boot, Spring Feign, Hystrix

We will use Spring Feign to make RESTful requests to the KYC service and integrate Hystrix into our service. The source
code is really straightforward, the **VerificationServiceClient** makes request to the KYC API which is consumed by the
**VerificationUserService** in order to return the verification result to customers via **VerificationUserEndpoint**.

Hystrix is enabled and configured via _application.yml_ with some default configurations:
```shell
hystrix:
  command:
    default:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 3000
      circuitBreaker:
        requestVolumeThreshold: 5
        sleepWindowInMilliseconds: 2000
        errorThresholdPercentage: 40
```
We will write an integration test (**VerificationServiceClientTest**) to verify how Hystrix works by a few scenarios.

## How to get started?
We can start this project via below gradle commands:
```shell
gradle build
gradle bootRun
```
To run the integration test
```shell
gradle build
gradle test
```

