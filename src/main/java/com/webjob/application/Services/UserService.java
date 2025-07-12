package com.webjob.application.Services;

import com.webjob.application.Models.User;
import com.webjob.application.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    public User handle(User user){
        // Mã hóa mật khẩu trước khi lưu
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
        }
        return userRepository.save(user);
    }

    public List<User> getAll(){
        return userRepository.findAll();
    }
    public boolean checkById(Long id) {
        boolean exists = userRepository.existsById(id);
        if (!exists) {
            throw new IllegalArgumentException("Không tồn tại User với ID: " + id);
        }
        return true;
    }


    public Optional<User> getbyID(Long id){
        return userRepository.findById(id);
    }
    public void delete(User user){
        userRepository.delete(user);
    }

    public User getbyEmail(String email) {
        return this.userRepository.findByEmail(email);
    }

}
