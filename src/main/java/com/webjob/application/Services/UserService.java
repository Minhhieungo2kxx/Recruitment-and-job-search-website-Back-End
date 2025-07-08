package com.webjob.application.Services;

import com.webjob.application.Models.User;
import com.webjob.application.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    public User handle(User user){
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

}
