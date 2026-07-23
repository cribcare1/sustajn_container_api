package com.sustajn.oderservice.service;

import com.sustajn.oderservice.dto.ContainerDetailsResponse;

public interface ContainerDetailsService {
    ContainerDetailsResponse getContainerDetails(Long productId);
}