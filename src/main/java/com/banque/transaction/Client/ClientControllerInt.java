package com.banque.transaction.Client;

import com.banque.transaction.ServerResponse.ServerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/client/")
public interface ClientControllerInt {
    @GetMapping("/all")
    ResponseEntity<List<Client>> findAllClient();
    @GetMapping("/findbyid/{id}")
    ResponseEntity<Client> findById(@PathVariable String id);
//    @PostMapping("/create")
//    ResponseEntity<ServerResponse> createClient(@RequestParam("client") String client);
//    @PutMapping("/update")
//    ResponseEntity<ServerResponse> updateClient(@RequestParam("client") String client);
    @DeleteMapping("/delete")
    ResponseEntity<ServerResponse> deleteClient(@PathVariable String id);
}
