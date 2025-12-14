package com.sustajn.oderservice.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface OrderService {
    public Map<String, Object> borrowContainer();
    public Map<String, Object> getActiveContainerTypes();
    public Map<String, Object> deleteContainerType(Integer id);
}
