package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MostAndLeastUsedContainerResponse {

    private List<MostUsedContainerResponse> mostUsedContainerResponse;
    private List<LeastUsedContainerResponse> leastUsedContainerResponse;
}
