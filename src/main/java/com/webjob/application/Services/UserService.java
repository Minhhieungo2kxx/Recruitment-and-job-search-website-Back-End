package com.webjob.application.Services;

import com.webjob.application.Models.User;
import com.webjob.application.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    @Transactional
    public User handle(User user){
        // 2. Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại trong hệ thống.");
        }
        // Mã hóa mật khẩu trước khi lưu
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
        }
        return userRepository.save(user);
    }
    @Transactional
    public User handleUpdate(User user){

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

    @Transactional
    public void delete(User user){
        userRepository.delete(user);
    }

    public User getbyEmail(String email) {
        return this.userRepository.findByEmail(email);
    }

    public Page<User> getAllPage(int page,int size){
        Sort.Direction direction=Sort.Direction.ASC;
        Sort sort=Sort.by(direction,"fullName");
        Pageable pageable= PageRequest.of(page,size,sort);
        return userRepository.findAll(pageable);


    }
}
