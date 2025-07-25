package com.webjob.application.Services;

import com.webjob.application.Models.Company;
import com.webjob.application.Models.User;
import com.webjob.application.Repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
        User user = this.userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        return user;
    }
    public User getEmailAndRefreshtoken(String email,String tokenrefresh) {
        User user =userRepository.findByEmailAndRefreshToken(email,tokenrefresh);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email or Refreshtoken,Please Check again");
        }
        return user;
    }


    public Page<User> getAllPage(int page,int size){
        Sort.Direction direction=Sort.Direction.ASC;
        Sort sort=Sort.by(direction,"fullName");
        Pageable pageable= PageRequest.of(page,size,sort);
        return userRepository.findAll(pageable);


    }
    @Transactional
    public void updateRefreshtoken(Long id, String refreshtoken){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        user.setRefreshToken(refreshtoken);
        userRepository.save(user);
    }
    public Optional<User> geybyCompany(Company company){
        return userRepository.findByCompany(company);
    }
}

///@Transactional là một annotation trong Spring (Spring Framework và Spring Boot),
// dùng để quản lý giao dịch (transaction) khi bạn làm việc với CSDL (thường là qua JPA/Hibernate).
//✅ Nó là gì?
//@Transactional đánh dấu một phương thức hoặc class sẽ được thực thi trong một giao dịch CSDL (database transaction).
//        🔍 Hiểu đơn giản:
//Giao dịch (transaction) là một đơn vị công việc phải thành công hoàn toàn hoặc thất bại hoàn toàn – không có trạng thái nửa vời.
//        📦 Tác dụng của @Transactional:
//        1. Tự động mở giao dịch khi bắt đầu method
//Spring sẽ tạo một transaction khi method được gọi.
//
//2. Theo dõi thay đổi của entity (managed state)
//Nếu bạn lấy entity từ DB, chỉnh sửa nó – Hibernate sẽ tự động cập nhật khi kết thúc method (dirty checking).
//
//        3. Tự động commit hoặc rollback
//Nếu method kết thúc bình thường, Spring commit giao dịch (lưu thay đổi vào DB).
//
//Nếu xảy ra lỗi (runtime exception), Spring rollback giao dịch (hủy bỏ toàn bộ thay đổi)./
