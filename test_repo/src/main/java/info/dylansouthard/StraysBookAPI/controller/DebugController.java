package info.dylansouthard.StraysBookAPI.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    @PatchMapping("/api/debug/echo")
    public ResponseEntity<String> echoBody(@RequestBody String rawBody) {
        System.out.println("ECHO: " + rawBody);
        return ResponseEntity.ok("You sent: " + rawBody);
    }
}
