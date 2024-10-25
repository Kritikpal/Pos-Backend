package com.kritik.POS.common.controller;

import com.kritik.POS.exception.errors.AppException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.kritik.POS.common.route.TestRoute.WELCOME;

@RestController
@Tag(name = "Test",description = "Test the server")
public class TestController {

    @GetMapping(WELCOME)
    public ResponseEntity<String> welcome() throws AppException {
        return ResponseEntity.ok("Welcome");
    }

}
