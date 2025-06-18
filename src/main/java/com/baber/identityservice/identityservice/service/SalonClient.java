package com.baber.identityservice.identityservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

//@Service
//public class SalonClient {
//    private final WebClient webClient;
//
//    @Autowired
//    public SalonClient(WebClient.Builder webClientBuilder) {
//        this.webClient = webClientBuilder.baseUrl("http://SALON-SERVICE").build();
//    }
//
//    public Salon getSalonById(Long salonId) {
//        return webClient.get()
//                .uri("/salons/{id}", salonId)
//                .retrieve()
//                .bodyToMono(Salon.class)
//                .block();
//    }
//}
