package io.github.emresurgun.benchmark.target.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UP");
    }

    @PostMapping("/upload")
    public ResponseEntity<Void> upload(@RequestBody(required = false) byte[] data) {
        return ResponseEntity.ok().build();
    }
}