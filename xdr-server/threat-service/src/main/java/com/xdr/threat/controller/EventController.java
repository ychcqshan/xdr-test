package com.xdr.threat.controller;

import com.xdr.common.dto.ApiResponse;
import com.xdr.threat.model.Event;
import com.xdr.threat.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final AlertService alertService;

    /** S-THR-001: Agent事件上报入口 */
    @PostMapping
    public ApiResponse<Event> receiveEvent(@RequestBody Map<String, Object> eventData) {
        return ApiResponse.ok(alertService.receiveEvent(eventData));
    }
}
