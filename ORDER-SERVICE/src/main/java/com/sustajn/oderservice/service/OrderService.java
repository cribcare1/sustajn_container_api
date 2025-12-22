package com.sustajn.oderservice.service;

import com.sustajn.oderservice.request.BorrowRequest;
import com.sustajn.oderservice.request.ReturnRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface OrderService {
    public Map<String, Object> borrowContainers(BorrowRequest request);
    public Map<String, Object> returnContainers(ReturnRequest request);
}
