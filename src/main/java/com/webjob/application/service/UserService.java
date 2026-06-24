package com.webjob.application.service;

import com.webjob.application.dto.Request.UserSetting;
import com.webjob.application.dto.Request.Userrequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.UserDTO;
import com.webjob.application.models.Entity.*;
import com.webjob.application.dto.Request.ChangePasswordRequest;

import com.webjob.application.repository.ConversationRepository;
import com.webjob.application.repository.MessageRepository;
import com.webjob.application.repository.ResumeRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.Redis.TokenBlacklistService;
import com.webjob.application.service.Socket.PresenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
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

    private final PresenceService presenceService;



    public User handleUser(User user) {
        // 2. Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại trong hệ thống.");
        }
        Optional<Company> company = companyService.getbyID(user.getCompany().getId());
        user.setCompany(company.orElse(null));
        Optional<Role> role = roleService.getByid(user.getRole().getId());
        user.setRole(role.orElse(null));
        // Mã hóa mật khẩu trước khi lưu
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
        }

        return userRepository.save(user);
    }


    public User handleUpdate(User user) {
        Optional<Company> company = companyService.getbyID(user.getCompany().getId());
        user.setCompany(company.orElse(null));
        Optional<Role> role = roleService.getByid(user.getRole().getId());
        user.setRole(role.orElse(null));
        // Mã hóa mật khẩu trước khi lưu
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
        }
        return userRepository.save(user);
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public boolean checkById(Long id) {
        boolean exists = userRepository.existsById(id);
        if (!exists) {
            throw new IllegalArgumentException("Không tồn tại User với ID: " + id);
        }
        return true;
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }


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
                .orElseThrow(() -> new UsernameNotFoundException("User not found with company: " + company.getName()));
        return user;
    }


    public User getUserByRefreshToken(String refreshToken) {
        User user = userRepository.findByRefreshToken(refreshToken);
        if (user == null) {
            throw new UsernameNotFoundException("Invalid refresh token or not found");
        }
        return user;
    }


    public Page<User> getAllPage(int page, int size) {
        Sort.Direction direction = Sort.Direction.ASC;
        Sort sort = Sort.by(direction, "fullName");
        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findAll(pageable);


    }

    @Transactional
    public void updateRefreshtoken(Long id, String refreshtoken) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        user.setRefreshToken(refreshtoken);
        userRepository.save(user);
    }

    public Optional<User> geybyCompany(Company company) {

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
        Page<User> pagelist = getAllPage(page - 1, size);
        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        List<User> userList = pagelist.getContent();
        List<UserDTO> userDTOList = new ArrayList<>();
        for (User user : userList) {
            UserDTO userDTO = modelMapper.map(user, UserDTO.class);
            userDTOList.add(userDTO);
        }
        ResponseDTO<?> respond = new ResponseDTO<>(metaDTO, userDTOList);
        return respond;
    }


    public void changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest, Authentication authentication) {

        User user = getById(Long.valueOf(authentication.getName()));
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
            long remaining = tokenBlacklistService.getRemainingValidity(token);
            tokenBlacklistService.blacklistToken(token, remaining);
        }

    }
    @Transactional
    public ResponseEntity<ApiResponse<UserDTO>> create_User(Userrequest userrequest) {

        // Tạo user và ánh xạ dữ liệu
        User user = modelMapper.map(userrequest, User.class);
        // Xử lý và phản hồi
        User userSaved = handleUser(user);
        UserDTO userDTO = modelMapper.map(userSaved, UserDTO.class);
        ApiResponse<UserDTO> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Create USER successful",
                userDTO
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

    @Transactional
    public ResponseEntity<?> edit_UserById( Long id,  Userrequest userrequest) {
        User user = getById(id);
        Instant instant = user.getCreatedAt();
        modelMapper.map(userrequest, user);
        user.setCreatedAt(instant);
        User updatedUser = handleUpdate(user);
        UserDTO userDTO = modelMapper.map(updatedUser, UserDTO.class);
        ApiResponse<UserDTO> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Edit USER successful",
                userDTO

        );
        return ResponseEntity.ok(response);
    }
    @Transactional
    public ResponseEntity<?> delete_UserbyId(@PathVariable Long id) {
        checkById(id);
        User edit = getById(id);
        deleteUser(edit);
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.NO_CONTENT.value(),
                null,
                "Delete USER successful",
                null

        );
        return ResponseEntity.ok(response);

    }
    public ResponseEntity<?> get_UserbyID( Long id) {

        checkById(id);
        User user = getById(id);
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        ApiResponse<?> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Detail USER successful",
                userDTO

        );
        return ResponseEntity.ok(response);

    }
    public ResponseEntity<?> Get_allPageList(String pageparam) {
        ResponseDTO<?> respond = getPaginatedResumes(pageparam, "default");
        ApiResponse<?> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "fetch all user",
                respond
        );
        return ResponseEntity.ok(response);

    }
    @Transactional
    public ResponseEntity<?> Setting_User(@Valid @RequestBody UserSetting userSetting, Authentication authentication) {

        User user = getById(Long.valueOf(authentication.getName()));
        Instant instant = user.getCreatedAt();
        modelMapper.map(userSetting, user);
        user.setCreatedAt(instant);
        User updatedUser = handleUpdate(user);
        UserSetting setting = modelMapper.map(updatedUser, UserSetting.class);
        ApiResponse<UserSetting> response = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Setting USER successful",
                setting
        );
        return ResponseEntity.ok(response);
    }
    @Transactional
    public ResponseEntity<?> change_forPassword(ChangePasswordRequest request, HttpServletRequest httprequest, Authentication authentication) {
        changePassword(request, httprequest, authentication);
        ApiResponse<UserSetting> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Thay doi mat khau successful,Vui long dang nhap lai",
                null

        );
        return ResponseEntity.ok(response);
    }

}



