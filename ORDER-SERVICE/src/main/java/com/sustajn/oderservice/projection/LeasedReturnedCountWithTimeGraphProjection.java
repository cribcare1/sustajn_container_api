package com.sustajn.oderservice.projection;

public interface LeasedReturnedCountWithTimeGraphProjection {
    String getTime();          // "0-1", "1-2"
    Integer getLeasedReturnedCount(); // mapped from COALESCE(SUM(...))
}
