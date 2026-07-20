package com.webjob.application.service;

import com.webjob.application.dto.Request.*;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.UserDTO;
import com.webjob.application.enums.UserStatus;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.exception.Customs.UnauthorizedException;
import com.webjob.application.models.Entity.*;

import com.webjob.application.repository.ConversationRepository;
import com.webjob.application.repository.MessageRepository;

import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.Redis.TokenBlacklistService;
import com.webjob.application.service.Socket.PresenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    //    private final ResumeRepository resumeRepository;
    private final CompanyService companyService;
    private final ModelMapper modelMapper;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final TokenBlacklistService tokenBlacklistService;

    private final PresenceService presenceService;


    public User handleUser(Userrequest userrequest) {

        if (userRepository.existsByEmailAndDeletedFalse(userrequest.getEmail())) {
            throw new IllegalArgumentException("Email: " + userrequest.getEmail() + " đã tồn tại trong hệ thống.");
        }

        User user = modelMapper.map(userrequest, User.class);
        Role role = roleService.getByid(userrequest.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
        user.setRole(role);
        String code = role.getCode();
        // Chỉ HR mới cần Company
        if (code.startsWith("HR")) {
            if (userrequest.getCompanyId() == null) {
                throw new RuntimeException("HR phải thuộc một công ty.");
            }

            Company company = companyService.getbyID(userrequest.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company không tồn tại"));

            user.setCompany(company);
        } else {
            // Admin/User không có company
            user.setCompany(null);
        }

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return userRepository.save(user);
    }
    public User registerClientUser(Userrequest userrequest) {

        if (userRepository.existsByEmailAndDeletedFalse(userrequest.getEmail())) {
            throw new IllegalArgumentException("Email: " + userrequest.getEmail() + " đã tồn tại trong hệ thống.");
        }

        User user = modelMapper.map(userrequest, User.class);
        Role role = roleService.getByid(3L)
                .orElse(null);
        user.setRole(role);
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }


    public User handleUpdate(Long id, UserRequestUpdate userrequest) {
        User user = getById(id);
        Instant instant = user.getCreatedAt();
        modelMapper.map(userrequest, user);
        user.setCreatedAt(instant);
        if (userrequest.getRoleId() != null) {
            Role role = roleService.getByid(userrequest.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại "));
            if (!role.isActive()) {
                throw new RuntimeException("Role đã bị vô hiệu hóa");
            }
            user.setRole(role);
        }
        if (user.getRole() == null) {
            throw new RuntimeException("User chưa có role");
        }
        String code = user.getRole().getCode();
// Chỉ HR mới cần Company
        if (code.startsWith("HR")) {
            if (userrequest.getCompanyId() == null) {
                throw new RuntimeException("HR phải thuộc một công ty.");
            }

            Company company = companyService.getbyID(userrequest.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company không tồn tại"));

            user.setCompany(company);
        } else {
            // Admin/User không có company
            user.setCompany(null);
        }

        return userRepository.save(user);
    }

    public User handleSetting(Long id, UserSetting userSetting) {

        User user = getById(id);
        Instant instant = user.getCreatedAt();
        modelMapper.map(userSetting, user);
        user.setCreatedAt(instant);

        return userRepository.save(user);
    }


    public boolean checkById(Long id) {
        boolean exists = userRepository.existsByIdAndDeletedFalse(id);
        if (!exists) {
            throw new ResourceNotFoundException("Không tồn tại User với ID: " + id);
        }
        return true;
    }



    public User getById(Long id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or no Active  with id: " + id));
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
        User user = userRepository.findByCompanyAndDeletedFalse(company);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with company: " + company.getName());
        }
        return user;
    }


    public User getUserByRefreshToken(String refreshToken) {
        User user = userRepository.findByRefreshTokenAndDeletedFalse(refreshToken);

        if (user == null) {
            throw new UsernameNotFoundException("Invalid refresh token or not found");
        }
        switch (user.getStatus()) {
            case ACTIVE:
                return user;

            case BLOCKED:
                throw new UnauthorizedException("Your account has been blocked.");

            case PENDING:
                throw new UnauthorizedException("Your account is pending verification.");

            case INACTIVE:
                throw new UnauthorizedException("Your account has been deactivated.");

            default:
                throw new UnauthorizedException("Invalid account status.");
        }
    }


    public Page<User> getAllPage(int page, int size) {
        Sort.Direction direction = Sort.Direction.ASC;
        Sort sort = Sort.by(direction, "id");
        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findAll(pageable);


    }

    @Transactional
    public void updateRefreshtoken(Long id, String refreshtoken) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        user.setRefreshToken(refreshtoken);
        userRepository.save(user);
    }


    public ResponseDTO<List<UserDTO>> getPaginatedResumes(int page, int size) {

        try {

            if (page <= 0) page = 1;
            if (size <= 0) size = 10;
        } catch (NumberFormatException e) {
            page = 1; // mặc định về trang đầu tiên nếu input không hợp lệ
            size = 10;
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
        ResponseDTO<List<UserDTO>> respond = new ResponseDTO<>(metaDTO, userDTOList);
        return respond;
    }


    @Transactional
    public void changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest, Authentication authentication) {

        User user = getCurrentUser();
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
    public UserDTO createUser(Userrequest userrequest) {
        User userSaved = handleUser(userrequest);
        UserDTO userDTO = modelMapper.map(userSaved, UserDTO.class);
        return userDTO;

    }


    @Transactional
    public UserDTO updateUser(Long id, UserRequestUpdate userrequest) {
        User updatedUser = handleUpdate(id, userrequest);
        UserDTO userDTO = modelMapper.map(updatedUser, UserDTO.class);
        return userDTO;

    }


    public UserDTO getUserByID(Long id) {
        checkById(id);
        User user = getById(id);
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        return userDTO;

    }


    public ResponseDTO<List<UserDTO>> getAllUser(int page, int size) {
        return getPaginatedResumes(page, size);
    }

    @Transactional
    public UserSetting settingUser(Long id, UserSetting userSetting) {
        User updatedUser = handleSetting(id, userSetting);
        UserSetting setting = modelMapper.map(updatedUser, UserSetting.class);
        return setting;
    }


    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.valueOf(authentication.getName());
        User user = getById(userId);
        switch (user.getStatus()) {
            case ACTIVE:
                return user;

            case BLOCKED:
                throw new UnauthorizedException("Your account has been blocked.");

            case PENDING:
                throw new UnauthorizedException("Your account is pending verification.");

            case INACTIVE:
                throw new UnauthorizedException("Your account has been deactivated.");

            default:
                throw new UnauthorizedException("Invalid account status.");
        }

    }

    public User updatePassword(Long id, PasswordRequest request) {

        User user = getById(id);

        if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        return userRepository.save(user);
    }

    @Transactional
    public UserDTO restoreUser(Long id) {
        User user = userRepository.findByIdAndDeletedTrue(id)
                .orElseThrow(() -> new BadRequestException("User not found or Delete "));
        boolean existed = userRepository.existsByEmailAndDeletedFalse(user.getEmail());
        if (existed) {
            throw new IllegalArgumentException(
                    "Email đã được sử dụng bởi tài khoản khác.");
        }
        user.setDeleted(false);
        user.setDeletedAt(null);
        user.setStatus(UserStatus.ACTIVE);
        user.setRefreshToken(null);
        UserDTO userDTO = modelMapper.map(userRepository.save(user), UserDTO.class);
        return userDTO;

    }

    @Transactional
    public void deleteUserById(Long id) {
        User user = getById(id);
        user.setDeleted(true);
        user.setDeletedAt(Instant.now());
        user.setOnline(false);
        user.setRefreshToken(null);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);


    }

}



