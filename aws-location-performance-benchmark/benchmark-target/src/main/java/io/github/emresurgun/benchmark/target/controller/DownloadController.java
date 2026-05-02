package io.github.emresurgun.benchmark.target.controller;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class DownloadController {

    @GetMapping("/download/{sizeKb}")
    public ResponseEntity<byte[]> download(@PathVariable int sizeKb)
    {
     byte[] payload = new byte[sizeKb * 1024];
     return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(payload);
    }

}
