package com.webjob.application.Controller;

import com.webjob.application.Models.User;
import com.webjob.application.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class UserController {

    private final UserService userService;


    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create/user")
    public ResponseEntity<?> create(@Valid @RequestBody User user) {
            User save = userService.handle(user);
            return new ResponseEntity<>(save, HttpStatus.CREATED);

    }

    @GetMapping("/all/user")
    public ResponseEntity<List<User>> getAllUser() {
        List<User> list = userService.getAll();
        if (list.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(list);
    }

    @PutMapping("user/edit/{id}")
    public ResponseEntity<?> editUserbyId(@PathVariable Long id, @Valid @RequestBody User user) {
        try {
            userService.checkById(id);
            Optional<User> canfind = userService.getbyID(id);
            User edit = canfind.get();
            edit.setEmail(user.getEmail());
            edit.setFullName(user.getFullName());
            edit.setPassword(user.getPassword());
            userService.handle(edit);
            return ResponseEntity.ok(edit);

        } catch (
                IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }
    @DeleteMapping("user/delete/{id}")
    public ResponseEntity<?> deleteUserbyId(@PathVariable Long id) {
        try {
            userService.checkById(id);
            Optional<User> canfind = userService.getbyID(id);
            User edit = canfind.get();
            userService.delete(edit);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

        } catch (
                IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }
    @GetMapping("user/detail/{id}")
    public ResponseEntity<?> getUserbyID(@PathVariable Long id){
        try {
            userService.checkById(id);
            Optional<User> findId=userService.getbyID(id);
            User userid=findId.get();
            return ResponseEntity.ok(userid);

        }catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());

        }


    }


}


//|         Cách viết                              | Khi nào dùng                                         |           | Ưu điểm                |
//        | -------------------------------------- | --------------------------------------------------- | --------------------------------- |
//        | `new ResponseEntity<>(body, status)`   | Khi bạn **cần chỉ định rõ ràng** status hoặc header | Linh hoạt, chính xác              |
//        | `ResponseEntity.ok(body)`              | Khi bạn trả về **HTTP 200 OK** đơn giản             | Gọn gàng, dễ đọc, phổ biến nhất   |
//        | `ResponseEntity.status(...).body(...)` | Khi bạn cần **status tùy chỉnh** nhưng vẫn rõ ràng  | Cân bằng giữa rõ ràng và ngắn gọn |
//
