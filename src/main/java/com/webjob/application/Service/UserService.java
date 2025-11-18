package com.webjob.application.Service;

import com.webjob.application.Dto.Response.MetaDTO;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Dto.Response.UserDTO;
import com.webjob.application.Model.Entity.*;
import com.webjob.application.Dto.Request.ChangePasswordRequest;

import com.webjob.application.Repository.ConversationRepository;
import com.webjob.application.Repository.MessageRepository;
import com.webjob.application.Repository.ResumeRepository;
import com.webjob.application.Repository.UserRepository;
import com.webjob.application.Service.Redis.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final ResumeRepository resumeRepository;
    private final CompanyService companyService;
    private final ModelMapper modelMapper;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final TokenBlacklistService tokenBlacklistService;



    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleService roleService, ResumeRepository resumeRepository, CompanyService companyService, ModelMapper modelMapper, MessageRepository messageRepository, ConversationRepository conversationRepository,TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
        this.resumeRepository = resumeRepository;
        this.companyService = companyService;
        this.modelMapper = modelMapper;
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.tokenBlacklistService = tokenBlacklistService;
    }
    @Transactional
    public User handle(User user){
        // 2. Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại trong hệ thống.");
        }
        Optional<Company> company=companyService.getbyID(user.getCompany().getId());
        user.setCompany(company.isPresent()?company.get():null);
        Optional<Role> role=roleService.getByid(user.getRole().getId());
        user.setRole(role.isPresent()?role.get():null);
        // Mã hóa mật khẩu trước khi lưu
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
        }

        return userRepository.save(user);
    }
    @Transactional
    public User handleUpdate(User user){
        Optional<Company> company=companyService.getbyID(user.getCompany().getId());
        user.setCompany(company.isPresent()?company.get():null);
        Optional<Role> role=roleService.getByid(user.getRole().getId());
        user.setRole(role.isPresent()?role.get():null);
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
    public void deleteUser(User user) {
        // Xóa message mà user là sender hoặc receiver
        List<Message> messages = messageRepository.findAllBySenderOrReceiver(user, user);
        messageRepository.deleteAll(messages);

        // Xóa conversation mà user là user1 hoặc user2
        List<Conversation> conversations = conversationRepository.findAllByUser1OrUser2(user, user);
        conversationRepository.deleteAll(conversations);

        // Nếu có Resume liên quan
        Page<Resume> resumes = resumeRepository.findAllByUser(user, null);
        List<Resume> resumeList = resumes.getContent();
        for (Resume resume : resumeList) {
            resume.setUser(null); // giữ lại Resume nhưng gỡ liên kết
        }
        resumeRepository.saveAll(resumeList);

        // Cuối cùng xóa user
        userRepository.delete(user);
    }



    public User getbyEmail(String email) {
        User user = this.userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        return user;
    }
    public User getEmailbyGoogle(String email) {
        User user = this.userRepository.findByEmail(email);
        return user;
    }
    public User getbyHR(Company company) {
        User user = this.userRepository.findByCompany(company)
                .orElseThrow(()->new UsernameNotFoundException("User not found with company: " +company.getName()));
        return user;
    }

//    public User getEmailAndRefreshtoken(String email,String tokenrefresh) {
//        User user =userRepository.findByEmailAndRefreshToken(email,tokenrefresh);
//        if (user == null) {
//            throw new UsernameNotFoundException("User not found with email or Refreshtoken,Please Check again");
//        }
//        return user;
//    }
public User getUserByRefreshToken(String refreshToken) {
    User user = userRepository.findByRefreshToken(refreshToken);
    if (user == null) {
        throw new UsernameNotFoundException("Invalid refresh token or not found");
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
    public ResponseDTO<?> getPaginatedResumes(String pageparam, String type) {
        int page = 1;
        int size = 8;
        try {
            page = Integer.parseInt(pageparam);
            if (page <= 0) page = 1;
        } catch (NumberFormatException e) {
            page = 1; // mặc định về trang đầu tiên nếu input không hợp lệ
        }
        Page<User> pagelist=getAllPage(page-1,size);
        int currentpage=pagelist.getNumber()+1;
        int pagesize=pagelist.getSize();
        int totalpage=pagelist.getTotalPages();
        Long totalItem=pagelist.getTotalElements();

        MetaDTO metaDTO=new MetaDTO(currentpage,pagesize,totalpage,totalItem);
        List<User> userList=pagelist.getContent();
        List<UserDTO> userDTOList=new ArrayList<>();
        for (User user:userList){
            UserDTO userDTO=modelMapper.map(user,UserDTO.class);
            userDTOList.add(userDTO);
        }
        ResponseDTO<?> respond=new ResponseDTO<>(metaDTO,userDTOList);
        return respond;
    }
    @Transactional
    public void changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest) {
        // Lấy thông tin người dùng hiện tại từ context
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = getbyEmail(userEmail);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không đúng");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("Xác nhận mật khẩu không khớp");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        AddBlacklistToken(httpRequest);


    }
    private void AddBlacklistToken(HttpServletRequest httpRequest) {
        String token = tokenBlacklistService.extractBearerToken(httpRequest);
        if (token != null) {
            long remaining =tokenBlacklistService.getRemainingValidity(token);
            tokenBlacklistService.blacklistToken(token, remaining);
        }

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
