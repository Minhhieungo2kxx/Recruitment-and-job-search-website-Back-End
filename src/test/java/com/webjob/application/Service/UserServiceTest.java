package com.webjob.application.Service;

import com.webjob.application.Model.Entity.*;
import com.webjob.application.Dto.Request.ChangePasswordRequest;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Dto.Response.UserDTO;
import com.webjob.application.Repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RoleService roleService;
    @Mock private ResumeRepository resumeRepository;
    @Mock private CompanyService companyService;
    @Mock private ModelMapper modelMapper;
    @Mock private MessageRepository messageRepository;
    @Mock private ConversationRepository conversationRepository;

    @InjectMocks
    private UserService userService;
    private User user;
    private Role role;
    private Company company;

    @BeforeEach
    void setup() {
        role = new Role();
        role.setId(1L);
        company = new Company();
        company.setId(1L);

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("rawPass");
        user.setRole(role);
        user.setCompany(company);
    }

    @Test
    void handle_ShouldSaveUser_WhenValidData() {
        // given
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(roleService.getByid(1L)).thenReturn(Optional.of(role));
        when(companyService.getbyID(1L)).thenReturn(Optional.of(company));
        when(passwordEncoder.encode("rawPass")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // when
        User saved = userService.handle(user);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getPassword()).isEqualTo("encodedPass");
        verify(userRepository).save(user);
    }

    @Test
    void handle_ShouldThrowException_WhenEmailExists() {
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);
        assertThatThrownBy(()->userService.handle(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email đã tồn tại trong hệ thống");

    }
    @Test
    void deleteUser_ShouldDeleteRelatedEntities() {
        when(messageRepository.findAllBySenderOrReceiver(user, user))
                .thenReturn(List.of(new Message()));
        when(conversationRepository.findAllByUser1OrUser2(user, user))
                .thenReturn(List.of(new Conversation()));
        when(resumeRepository.findAllByUser(eq(user), any()))
                .thenReturn(new PageImpl<>(List.of(new Resume())));

        userService.deleteUser(user);

        verify(messageRepository).deleteAll(anyList());
        verify(conversationRepository).deleteAll(anyList());
        verify(resumeRepository).saveAll(anyList());
        verify(userRepository).delete(user);
    }
    @Test
    void changePassword_ShouldUpdatePassword_WhenOldPasswordCorrect() {
        // given
        user.setPassword("encodedOldPass");
        when(passwordEncoder.matches("oldPass", "encodedOldPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(user.getEmail(), null));

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPass");
        request.setNewPassword("newPass");
        request.setConfirmNewPassword("newPass");

        // when
        userService.changePassword(request);

        // then
        assertThat(user.getPassword()).isEqualTo("encodedNewPass");
        verify(userRepository).save(user);
    }
    @Test
    void getbyEmail_ShouldThrow_WhenUserNotFound() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(null);

        assertThatThrownBy(() -> userService.getbyEmail("notfound@example.com"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }
    @Test
    void getPaginatedResumes_ShouldReturnResponseDTO() {
        UserDTO userDTO = new UserDTO();
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(modelMapper.map(any(User.class), eq(UserDTO.class))).thenReturn(userDTO);

        ResponseDTO<?> response = userService.getPaginatedResumes("1", "type");

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isInstanceOf(List.class);
    }
    @Test
    void updateRefreshtoken_ShouldUpdate_WhenUserExists() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        String newToken = "newRefreshToken";

        // when
        userService.updateRefreshtoken(1L, newToken);

        // then
        assertThat(user.getRefreshToken()).isEqualTo(newToken);
        verify(userRepository).save(user);
    }
    @Test
    void updateRefreshtoken_ShouldThrow_WhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateRefreshtoken(99L, "token"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
                .hasMessageContaining("User not found with id: 99");
    }






}
// Khuyến nghị dài hạn:
//
// Dùng constructor injection thay vì @Autowired field. Nó giúp:
//
//Dễ test hơn
//
//Giảm lỗi NullPointerException
//
//Dễ hiểu hơn cho framework và Mockito
