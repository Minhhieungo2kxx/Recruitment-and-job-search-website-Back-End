package com.webjob.application.service;

import com.webjob.application.dto.Request.*;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.UserDTO;
import com.webjob.application.enums.AccountType;
import com.webjob.application.enums.UserStatus;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.exception.Customs.UnauthorizedException;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.models.Entity.Role;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.ConversationRepository;
import com.webjob.application.repository.MessageRepository;
import com.webjob.application.repository.RoleRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.Redis.TokenBlacklistService;
import com.webjob.application.service.Socket.PresenceService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
/**
 * Unit test cho UserService.
 *
 * Không sử dụng @SpringBootTest vì đây là pure unit test.
 * Tất cả dependency của UserService đều được mock.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleService roleService;
    @Mock
    private CompanyService companyService;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PresenceService presenceService;
    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================
    // Test data helpers
    // =========================================================
    private User createUser() {
        User user = new User();

        user.setId(1L);
        user.setEmail("test@gmail.com");
        user.setPassword("encoded-password");
        user.setFullName("Test User");
        user.setDateOfBirth(LocalDate.of(2000, 1, 1));
        user.setGender("MALE");
        user.setAddress("Hai Phong");
        user.setStatus(UserStatus.ACTIVE);
        user.setDeleted(false);
        user.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        user.setUpdatedAt(Instant.parse("2025-01-02T00:00:00Z"));
        user.setOnline(true);
        user.setRefreshToken("refresh-token");

        return user;
    }

    private Role createRole(String code, boolean active) {
        Role role = new Role();

        role.setCode(code);
        role.setActive(active);

        return role;
    }

    private Company createCompany() {
        Company company = new Company();

        company.setId(10L);
        company.setName("ABC Company");

        return company;
    }

    private void mockActiveUser() {
        User user = createUser();

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getId().toString(),
                        null,
                        Collections.emptyList()
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        given(userRepository.findByIdAndDeletedFalse(user.getId()))
                .willReturn(Optional.of(user));
    }

    // handleUser()
    // =========================================================
    @Nested
    @DisplayName("handleUser()")
    class HandleUserTests {
        @Test
        @DisplayName("Should create USER successfully")
        void shouldCreateUserSuccessfully() {

            UserrequestAdmin request = new UserrequestAdmin();
            request.setEmail("user@gmail.com");
            request.setPassword("password123");
            request.setRoleId(1L);
            request.setCompanyId(null);

            Role role = createRole("USER", true);

            User mappedUser = createUser();
            mappedUser.setEmail(request.getEmail());
            mappedUser.setPassword(request.getPassword());

            User savedUser = createUser();

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(false);

            given(modelMapper.map(request, User.class))
                    .willReturn(mappedUser);

            given(roleService.getByid(1L))
                    .willReturn(Optional.of(role));

            given(passwordEncoder.encode("password123"))
                    .willReturn("encoded-password");

            given(userRepository.save(mappedUser))
                    .willReturn(savedUser);

            User result = userService.handleUser(request);

            assertThat(result).isSameAs(savedUser);
            assertThat(mappedUser.getRole()).isSameAs(role);
            assertThat(mappedUser.getCompany()).isNull();
            assertThat(mappedUser.getPassword())
                    .isEqualTo("encoded-password");

            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(mappedUser);
        }

        @Test
        @DisplayName("Should create HR successfully with company")
        void shouldCreateHrSuccessfully() {

            UserrequestAdmin request = new UserrequestAdmin();
            request.setEmail("hr@gmail.com");
            request.setPassword("password123");
            request.setRoleId(2L);
            request.setCompanyId(10L);

            Role hrRole = createRole("HR", true);
            Company company = createCompany();

            User mappedUser = createUser();

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(false);

            given(modelMapper.map(request, User.class))
                    .willReturn(mappedUser);

            given(roleService.getByid(2L))
                    .willReturn(Optional.of(hrRole));

            given(companyService.getbyID(10L))
                    .willReturn(Optional.of(company));

            given(passwordEncoder.encode(anyString()))
                    .willReturn("encoded");

            given(userRepository.save(mappedUser))
                    .willReturn(mappedUser);

            User result = userService.handleUser(request);

            assertThat(result).isSameAs(mappedUser);
            assertThat(mappedUser.getRole()).isSameAs(hrRole);
            assertThat(mappedUser.getCompany()).isSameAs(company);

            verify(companyService).getbyID(10L);
            verify(userRepository).save(mappedUser);
        }

        @Test
        @DisplayName("Should throw when email already exists")
        void shouldThrowWhenEmailAlreadyExists() {

            UserrequestAdmin request = new UserrequestAdmin();
            request.setEmail("existing@gmail.com");

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(true);

            assertThatThrownBy(() -> userService.handleUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("existing@gmail.com");

            verify(modelMapper, never()).map(any(), eq(User.class));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when HR has no company")
        void shouldThrowWhenHrHasNoCompany() {

            UserrequestAdmin request = new UserrequestAdmin();
            request.setEmail("hr@gmail.com");
            request.setRoleId(2L);
            request.setCompanyId(null);

            Role hrRole = createRole("HR", true);

            User mappedUser = createUser();

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(false);

            given(modelMapper.map(request, User.class))
                    .willReturn(mappedUser);

            given(roleService.getByid(2L))
                    .willReturn(Optional.of(hrRole));

            assertThatThrownBy(() -> userService.handleUser(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("HR phải thuộc một công ty");

            verify(companyService, never()).getbyID(anyLong());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when company does not exist")
        void shouldThrowWhenCompanyDoesNotExist() {

            UserrequestAdmin request = new UserrequestAdmin();
            request.setEmail("hr@gmail.com");
            request.setRoleId(2L);
            request.setCompanyId(10L);

            Role hrRole = createRole("HR", true);

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(false);

            given(modelMapper.map(request, User.class))
                    .willReturn(createUser());

            given(roleService.getByid(2L))
                    .willReturn(Optional.of(hrRole));

            given(companyService.getbyID(10L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.handleUser(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Company không tồn tại");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when role does not exist")
        void shouldThrowWhenRoleDoesNotExist() {

            UserrequestAdmin request = new UserrequestAdmin();
            request.setEmail("test@gmail.com");
            request.setRoleId(999L);

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(false);

            given(modelMapper.map(request, User.class))
                    .willReturn(createUser());

            given(roleService.getByid(999L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.handleUser(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Role không tồn tại");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not encode blank password")
        void shouldNotEncodeBlankPassword() {

            UserrequestAdmin request = new UserrequestAdmin();
            request.setEmail("test@gmail.com");
            request.setRoleId(1L);
            request.setCompanyId(null);

            Role role = createRole("USER", true);

            User user = createUser();
            user.setPassword(" ");

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(false);

            given(modelMapper.map(request, User.class))
                    .willReturn(user);

            given(roleService.getByid(1L))
                    .willReturn(Optional.of(role));

            given(userRepository.save(user))
                    .willReturn(user);

            userService.handleUser(request);

            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // =========================================================
    // registerClientUser()
    // =========================================================
    @Nested
    @DisplayName("registerClientUser()")
    class RegisterClientUserTests {

        @Test
        @DisplayName("Should register USER successfully")
        void shouldRegisterUserSuccessfully() {

            Userrequest request = new Userrequest();
            request.setEmail("user@gmail.com");
            request.setPassword("password123");
            request.setAccountType(AccountType.USER);

            User user = createUser();

            Role userRole = createRole("USER", true);

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(false);

            given(modelMapper.map(request, User.class))
                    .willReturn(user);

            given(roleRepository.findByCodeAndActiveTrue("USER"))
                    .willReturn(Optional.of(userRole));

            given(passwordEncoder.encode(anyString()))
                    .willReturn("encoded-password");

            given(userRepository.save(user))
                    .willReturn(user);

            User result = userService.registerClientUser(request);

            assertThat(result).isSameAs(user);
            assertThat(user.getRole()).isSameAs(userRole);
            assertThat(user.getCompany()).isNull();

            verify(roleRepository).findByCodeAndActiveTrue("USER");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should register HR successfully")
        void shouldRegisterHrSuccessfully() {

            Userrequest request = new Userrequest();
            request.setEmail("hr@gmail.com");
            request.setPassword("password123");
            request.setAccountType(AccountType.HR);
            request.setCompanyId(10L);

            User user = createUser();
            Company company = createCompany();
            Role hrRole = createRole("HR", true);

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(false);

            given(modelMapper.map(request, User.class))
                    .willReturn(user);

            given(companyService.getbyID(10L))
                    .willReturn(Optional.of(company));

            given(roleRepository.findByCodeAndActiveTrue("HR"))
                    .willReturn(Optional.of(hrRole));

            given(passwordEncoder.encode(anyString()))
                    .willReturn("encoded");

            given(userRepository.save(user))
                    .willReturn(user);

            User result = userService.registerClientUser(request);

            assertThat(result).isSameAs(user);
            assertThat(user.getCompany()).isSameAs(company);
            assertThat(user.getRole()).isSameAs(hrRole);
        }

        @Test
        @DisplayName("Should throw when duplicate email")
        void shouldThrowWhenDuplicateEmail() {

            Userrequest request = new Userrequest();
            request.setEmail("duplicate@gmail.com");

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(true);

            assertThatThrownBy(() -> userService.registerClientUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duplicate@gmail.com");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when HR company is missing")
        void shouldThrowWhenHrCompanyMissing() {

            Userrequest request = new Userrequest();
            request.setEmail("hr@gmail.com");
            request.setAccountType(AccountType.HR);
            request.setCompanyId(null);

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(false);

            given(modelMapper.map(request, User.class))
                    .willReturn(createUser());

            assertThatThrownBy(() -> userService.registerClientUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("HR phải chọn công ty");

            verify(companyService, never()).getbyID(anyLong());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when HR company does not exist")
        void shouldThrowWhenHrCompanyDoesNotExist() {

            Userrequest request = new Userrequest();
            request.setEmail("hr@gmail.com");
            request.setAccountType(AccountType.HR);
            request.setCompanyId(10L);

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(false);

            given(modelMapper.map(request, User.class))
                    .willReturn(createUser());

            given(companyService.getbyID(10L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.registerClientUser(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Company không tồn tại");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when USER role does not exist")
        void shouldThrowWhenUserRoleDoesNotExist() {

            Userrequest request = new Userrequest();
            request.setEmail("user@gmail.com");
            request.setAccountType(AccountType.USER);

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(false);

            given(modelMapper.map(request, User.class))
                    .willReturn(createUser());

            given(roleRepository.findByCodeAndActiveTrue("USER"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.registerClientUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Không tìm thấy role USER");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when HR role does not exist")
        void shouldThrowWhenHrRoleDoesNotExist() {

            Userrequest request = new Userrequest();
            request.setEmail("hr@gmail.com");
            request.setAccountType(AccountType.HR);
            request.setCompanyId(10L);

            given(userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
                    .willReturn(false);

            given(modelMapper.map(request, User.class))
                    .willReturn(createUser());

            given(companyService.getbyID(10L))
                    .willReturn(Optional.of(createCompany()));

            given(roleRepository.findByCodeAndActiveTrue("HR"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.registerClientUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Không tìm thấy role HR");

            verify(userRepository, never()).save(any());
        }
        // =========================================================
        // handleUpdate()
        // =========================================================

        @Nested
        @DisplayName("handleUpdate()")
        class HandleUpdateTests {

            @Test
            @DisplayName("Should update USER successfully and clear company")
            void shouldUpdateUserSuccessfully() {

                User user = createUser();

                Company oldCompany = createCompany();
                user.setCompany(oldCompany);

                Role oldRole = createRole("USER", true);
                user.setRole(oldRole);

                UserRequestUpdate request = new UserRequestUpdate();

                Instant createdAt = user.getCreatedAt();

                given(userRepository.findByIdAndDeletedFalse(1L))
                        .willReturn(Optional.of(user));

                given(userRepository.save(user))
                        .willReturn(user);

                User result = userService.handleUpdate(1L, request);

                assertThat(result).isSameAs(user);
                assertThat(user.getCreatedAt()).isEqualTo(createdAt);
                assertThat(user.getCompany()).isNull();

                verify(userRepository).save(user);
            }

            @Test
            @DisplayName("Should update role when roleId is provided")
            void shouldUpdateRoleWhenRoleIdProvided() {

                User user = createUser();
                Role oldRole = createRole("USER", true);
                Role newRole = createRole("ADMIN", true);

                user.setRole(oldRole);

                UserRequestUpdate request = new UserRequestUpdate();
                request.setRoleId(2L);

                given(userRepository.findByIdAndDeletedFalse(1L))
                        .willReturn(Optional.of(user));

                given(roleService.getByid(2L))
                        .willReturn(Optional.of(newRole));

                given(userRepository.save(user))
                        .willReturn(user);

                User result = userService.handleUpdate(1L, request);

                assertThat(result.getRole()).isSameAs(newRole);
            }

            @Test
            @DisplayName("Should throw when new role is inactive")
            void shouldThrowWhenRoleInactive() {

                User user = createUser();
                user.setRole(createRole("USER", true));

                Role inactiveRole = createRole("ADMIN", false);

                UserRequestUpdate request = new UserRequestUpdate();
                request.setRoleId(2L);

                given(userRepository.findByIdAndDeletedFalse(1L))
                        .willReturn(Optional.of(user));

                given(roleService.getByid(2L))
                        .willReturn(Optional.of(inactiveRole));

                assertThatThrownBy(() -> userService.handleUpdate(1L, request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Role đã bị vô hiệu hóa");

                verify(userRepository, never()).save(any());
            }

            @Test
            @DisplayName("Should throw when role does not exist")
            void shouldThrowWhenRoleDoesNotExist() {

                User user = createUser();
                user.setRole(createRole("USER", true));

                UserRequestUpdate request = new UserRequestUpdate();
                request.setRoleId(999L);

                given(userRepository.findByIdAndDeletedFalse(1L))
                        .willReturn(Optional.of(user));

                given(roleService.getByid(999L))
                        .willReturn(Optional.empty());

                assertThatThrownBy(() -> userService.handleUpdate(1L, request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Role không tồn tại");

                verify(userRepository, never()).save(any());
            }

            @Test
            @DisplayName("Should throw when user has no role")
            void shouldThrowWhenUserHasNoRole() {

                User user = createUser();
                user.setRole(null);

                UserRequestUpdate request = new UserRequestUpdate();

                given(userRepository.findByIdAndDeletedFalse(1L))
                        .willReturn(Optional.of(user));

                assertThatThrownBy(() -> userService.handleUpdate(1L, request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("User chưa có role");

                verify(userRepository, never()).save(any());
            }

            @Test
            @DisplayName("Should require company when updating HR")
            void shouldRequireCompanyForHr() {

                User user = createUser();
                user.setRole(createRole("HR", true));

                UserRequestUpdate request = new UserRequestUpdate();
                request.setCompanyId(null);

                given(userRepository.findByIdAndDeletedFalse(1L))
                        .willReturn(Optional.of(user));

                assertThatThrownBy(() -> userService.handleUpdate(1L, request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("HR phải thuộc một công ty");

                verify(userRepository, never()).save(any());
            }

            @Test
            @DisplayName("Should update HR company successfully")
            void shouldUpdateHrCompanySuccessfully() {

                User user = createUser();
                user.setRole(createRole("HR", true));

                Company company = createCompany();

                UserRequestUpdate request = new UserRequestUpdate();
                request.setCompanyId(10L);

                given(userRepository.findByIdAndDeletedFalse(1L))
                        .willReturn(Optional.of(user));

                given(companyService.getbyID(10L))
                        .willReturn(Optional.of(company));

                given(userRepository.save(user))
                        .willReturn(user);

                User result = userService.handleUpdate(1L, request);

                assertThat(result.getCompany()).isSameAs(company);
            }

            @Test
            @DisplayName("Should throw when HR company does not exist")
            void shouldThrowWhenHrCompanyDoesNotExist() {

                User user = createUser();
                user.setRole(createRole("HR", true));

                UserRequestUpdate request = new UserRequestUpdate();
                request.setCompanyId(10L);

                given(userRepository.findByIdAndDeletedFalse(1L))
                        .willReturn(Optional.of(user));

                given(companyService.getbyID(10L))
                        .willReturn(Optional.empty());

                assertThatThrownBy(() -> userService.handleUpdate(1L, request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Company không tồn tại");

                verify(userRepository, never()).save(any());
            }
        }

    }
    // =========================================================
    // handleSetting()
    // =========================================================

    @Test
    @DisplayName("handleSetting - should update setting and preserve createdAt")
    void handleSettingShouldPreserveCreatedAt() {

        User user = createUser();

        UserSetting setting = new UserSetting();

        Instant originalCreatedAt = user.getCreatedAt();

        given(userRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.of(user));

        given(userRepository.save(user))
                .willReturn(user);

        User result = userService.handleSetting(1L, setting);

        assertThat(result).isSameAs(user);
        assertThat(result.getCreatedAt()).isEqualTo(originalCreatedAt);

        verify(modelMapper).map(setting, user);
        verify(userRepository).save(user);
    }

    // =========================================================
    // checkById()
    // =========================================================

    @Test
    @DisplayName("checkById - should return true when user exists")
    void checkByIdShouldReturnTrue() {

        given(userRepository.existsByIdAndDeletedFalse(1L))
                .willReturn(true);

        boolean result = userService.checkById(1L);

        assertThat(result).isTrue();

        verify(userRepository).existsByIdAndDeletedFalse(1L);
    }

    @Test
    @DisplayName("checkById - should throw when user does not exist")
    void checkByIdShouldThrowWhenUserDoesNotExist() {

        given(userRepository.existsByIdAndDeletedFalse(1L))
                .willReturn(false);

        assertThatThrownBy(() -> userService.checkById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1");

        verify(userRepository, never()).findByIdAndDeletedFalse(anyLong());
    }
    // =========================================================
    // getById()
    // =========================================================

    @Test
    @DisplayName("getById - should return active user")
    void getByIdShouldReturnUser() {

        User user = createUser();

        given(userRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.of(user));

        User result = userService.getById(1L);

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("getById - should throw when not found")
    void getByIdShouldThrowWhenNotFound() {

        given(userRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1");
    }


    // =========================================================
    // getbyEmail()
    // =========================================================

    @Test
    @DisplayName("getbyEmail - should return user")
    void getByEmailShouldReturnUser() {

        User user = createUser();

        given(userRepository.findByEmail("test@gmail.com"))
                .willReturn(user);

        User result = userService.getbyEmail("test@gmail.com");

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("getbyEmail - should throw when not found")
    void getByEmailShouldThrowWhenNotFound() {

        given(userRepository.findByEmail("notfound@gmail.com"))
                .willReturn(null);

        assertThatThrownBy(
                () -> userService.getbyEmail("notfound@gmail.com")
        )
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("notfound@gmail.com");
    }

    // =========================================================
    // getEmailbyGoogle()
    // =========================================================

    @Test
    @DisplayName("getEmailbyGoogle - should return user")
    void getEmailByGoogleShouldReturnUser() {

        User user = createUser();

        given(userRepository.findByEmail("google@gmail.com"))
                .willReturn(user);

        User result = userService.getEmailbyGoogle("google@gmail.com");

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("getEmailbyGoogle - should return null when not found")
    void getEmailByGoogleShouldReturnNull() {

        given(userRepository.findByEmail("google@gmail.com"))
                .willReturn(null);

        User result = userService.getEmailbyGoogle("google@gmail.com");

        assertThat(result).isNull();
    }


    // =========================================================
    // getbyHR()
    // =========================================================

    @Test
    @DisplayName("getbyHR - should return HR")
    void getByHrShouldReturnUser() {

        Company company = createCompany();
        User user = createUser();
        user.setCompany(company);

        given(userRepository.findByCompanyAndDeletedFalse(company))
                .willReturn(user);

        User result = userService.getbyHR(company);

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("getbyHR - should throw when no HR exists")
    void getByHrShouldThrowWhenNotFound() {

        Company company = createCompany();

        given(userRepository.findByCompanyAndDeletedFalse(company))
                .willReturn(null);

        assertThatThrownBy(() -> userService.getbyHR(company))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(company.getName());
    }


    // =========================================================
    // getUserByRefreshTokenHash()
    // =========================================================

    @Test
    @DisplayName("refresh token - ACTIVE should return user")
    void refreshTokenActiveShouldReturnUser() {

        User user = createUser();
        user.setStatus(UserStatus.ACTIVE);

        given(userRepository.findByRefreshTokenAndDeletedFalse("hash"))
                .willReturn(user);

        User result = userService.getUserByRefreshTokenHash("hash");

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("refresh token - BLOCKED should throw UnauthorizedException")
    void refreshTokenBlockedShouldThrow() {

        User user = createUser();
        user.setStatus(UserStatus.BLOCKED);

        given(userRepository.findByRefreshTokenAndDeletedFalse("hash"))
                .willReturn(user);

        assertThatThrownBy(
                () -> userService.getUserByRefreshTokenHash("hash")
        )
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("blocked");
    }

    @Test
    @DisplayName("refresh token - PENDING should throw UnauthorizedException")
    void refreshTokenPendingShouldThrow() {

        User user = createUser();
        user.setStatus(UserStatus.PENDING);

        given(userRepository.findByRefreshTokenAndDeletedFalse("hash"))
                .willReturn(user);

        assertThatThrownBy(
                () -> userService.getUserByRefreshTokenHash("hash")
        )
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("pending");
    }

    @Test
    @DisplayName("refresh token - INACTIVE should throw UnauthorizedException")
    void refreshTokenInactiveShouldThrow() {

        User user = createUser();
        user.setStatus(UserStatus.INACTIVE);

        given(userRepository.findByRefreshTokenAndDeletedFalse("hash"))
                .willReturn(user);

        assertThatThrownBy(
                () -> userService.getUserByRefreshTokenHash("hash")
        )
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    @DisplayName("refresh token - should throw when token not found")
    void refreshTokenShouldThrowWhenNotFound() {

        given(userRepository.findByRefreshTokenAndDeletedFalse("invalid"))
                .willReturn(null);

        assertThatThrownBy(
                () -> userService.getUserByRefreshTokenHash("invalid")
        )
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Invalid refresh token");
    }


    // =========================================================
    // getAllPage()
    // =========================================================

    @Test
    @DisplayName("getAllPage - should request correct pagination")
    void getAllPageShouldRequestCorrectPagination() {

        Page<User> page = new PageImpl<>(List.of(createUser()));

        given(userRepository.findAll(any(Pageable.class)))
                .willReturn(page);

        Page<User> result = userService.getAllPage(0, 10);

        assertThat(result).isSameAs(page);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(userRepository).findAll(captor.capture());

        Pageable pageable = captor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
    }
    // =========================================================
    // updateRefreshtoken()
    // =========================================================

    @Test
    @DisplayName("updateRefreshtoken - should update refresh token")
    void updateRefreshTokenShouldUpdate() {

        User user = createUser();

        given(userRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.of(user));

        userService.updateRefreshtoken(1L, "new-refresh-token");

        assertThat(user.getRefreshToken())
                .isEqualTo("new-refresh-token");

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateRefreshtoken - should throw when user not found")
    void updateRefreshTokenShouldThrowWhenNotFound() {

        given(userRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(
                () -> userService.updateRefreshtoken(1L, "token")
        )
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("1");

        verify(userRepository, never()).save(any());
    }

    // =========================================================
    // getPaginatedResumes()
    // =========================================================

    @Test
    @DisplayName("getPaginatedResumes - should return mapped DTO page")
    void getPaginatedResumesShouldReturnDtoPage() {

        User user = createUser();

        Page<User> page = new PageImpl<>(
                List.of(user),
                PageRequest.of(0, 10),
                1
        );

        UserDTO dto = new UserDTO();

        given(userRepository.findAll(any(Pageable.class)))
                .willReturn(page);

        given(modelMapper.map(user, UserDTO.class))
                .willReturn(dto);

        ResponseDTO<List<UserDTO>> result =
                userService.getPaginatedResumes(1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getResult()).containsExactly(dto);

        verify(modelMapper).map(user, UserDTO.class);
    }

    @Test
    @DisplayName("getPaginatedResumes - page <= 0 should use page 1")
    void getPaginatedResumesShouldNormalizeInvalidPage() {

        Page<User> page = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 10),
                0
        );

        given(userRepository.findAll(any(Pageable.class)))
                .willReturn(page);

        userService.getPaginatedResumes(0, 10);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(userRepository).findAll(captor.capture());

        assertThat(captor.getValue().getPageNumber())
                .isEqualTo(0);
    }

    @Test
    @DisplayName("getPaginatedResumes - size <= 0 should use size 10")
    void getPaginatedResumesShouldNormalizeInvalidSize() {

        Page<User> page = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 10),
                0
        );

        given(userRepository.findAll(any(Pageable.class)))
                .willReturn(page);

        userService.getPaginatedResumes(1, 0);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(userRepository).findAll(captor.capture());

        assertThat(captor.getValue().getPageSize())
                .isEqualTo(1);
    }

    // =========================================================
    // changePassword()
    // =========================================================

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() {

            User user = createUser();
            user.setPassword("old-encoded");

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("old-password");
            request.setNewPassword("new-password");
            request.setConfirmNewPassword("new-password");

            HttpServletRequest httpRequest = mock(HttpServletRequest.class);

            given(userRepository.findByIdAndDeletedFalse(1L))
                    .willReturn(Optional.of(user));

            mockActiveUser();

            // getCurrentUser() will call repository again.
            given(userRepository.findByIdAndDeletedFalse(1L))
                    .willReturn(Optional.of(user));

            given(passwordEncoder.matches(
                    "old-password",
                    "old-encoded"
            )).willReturn(true);

            given(passwordEncoder.encode("new-password"))
                    .willReturn("new-encoded");

            given(tokenBlacklistService.extractBearerToken(httpRequest))
                    .willReturn("access-token");

            given(tokenBlacklistService.getRemainingValidity("access-token"))
                    .willReturn(100L);

            userService.changePassword(
                    request,
                    httpRequest,
                    mock(Authentication.class)
            );

            assertThat(user.getPassword())
                    .isEqualTo("new-encoded");

            verify(userRepository).save(user);
            verify(tokenBlacklistService)
                    .blacklistToken("access-token", 100L);
        }

        @Test
        @DisplayName("Should throw when old password is incorrect")
        void shouldThrowWhenOldPasswordIncorrect() {

            User user = createUser();
            user.setPassword("old-encoded");

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("wrong");
            request.setNewPassword("new-password");
            request.setConfirmNewPassword("new-password");

            HttpServletRequest httpRequest =
                    mock(HttpServletRequest.class);

            mockActiveUser();

            given(passwordEncoder.matches(
                    "wrong",
                    "encoded-password"
            )).willReturn(false);

            assertThatThrownBy(() ->
                    userService.changePassword(
                            request,
                            httpRequest,
                            mock(Authentication.class)
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mật khẩu cũ không đúng");

            verify(userRepository, never()).save(any());
            verify(tokenBlacklistService, never())
                    .blacklistToken(anyString(), anyLong());
        }

        @Test
        @DisplayName("Should throw when confirmation does not match")
        void shouldThrowWhenConfirmationDoesNotMatch() {

            User user = createUser();
            user.setPassword("old-encoded");

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("old-password");
            request.setNewPassword("new-password");
            request.setConfirmNewPassword("different-password");

            HttpServletRequest httpRequest =
                    mock(HttpServletRequest.class);

            mockActiveUser();

            given(passwordEncoder.matches(
                    "old-password",
                    "encoded-password"
            )).willReturn(true);

            assertThatThrownBy(() ->
                    userService.changePassword(
                            request,
                            httpRequest,
                            mock(Authentication.class)
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Xác nhận mật khẩu không khớp");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when new password equals old password")
        void shouldThrowWhenNewPasswordEqualsOldPassword() {

            User user = createUser();
            user.setPassword("old-encoded");

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("same-password");
            request.setNewPassword("same-password");
            request.setConfirmNewPassword("same-password");

            HttpServletRequest httpRequest =
                    mock(HttpServletRequest.class);

            mockActiveUser();

            given(passwordEncoder.matches(
                    "same-password",
                    "encoded-password"
            )).willReturn(true);

            assertThatThrownBy(() ->
                    userService.changePassword(
                            request,
                            httpRequest,
                            mock(Authentication.class)
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "Mật khẩu mới không được trùng"
                    );

            verify(passwordEncoder, never())
                    .encode(anyString());

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not blacklist when access token is absent")
        void shouldNotBlacklistWhenTokenAbsent() {

            User user = createUser();
            user.setPassword("encoded-password");

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("old");
            request.setNewPassword("new");
            request.setConfirmNewPassword("new");

            HttpServletRequest httpRequest = mock(HttpServletRequest.class);

            mockActiveUser();

            given(userRepository.findByIdAndDeletedFalse(user.getId()))
                    .willReturn(Optional.of(user));

            given(passwordEncoder.matches("old", "encoded-password"))
                    .willReturn(true);

            given(passwordEncoder.encode("new"))
                    .willReturn("new-encoded");

            given(tokenBlacklistService.extractBearerToken(httpRequest))
                    .willReturn(null);

            userService.changePassword(
                    request,
                    httpRequest,
                    mock(Authentication.class)
            );

            verify(userRepository).save(user);

            verify(tokenBlacklistService, never())
                    .getRemainingValidity(anyString());

            verify(tokenBlacklistService, never())
                    .blacklistToken(anyString(), anyLong());
        }

    }
    // =========================================================
    // createUser()
    // =========================================================

    @Test
    @DisplayName("createUser - should map saved user to DTO")
    void createUserShouldMapSavedUser() {

        UserrequestAdmin request = new UserrequestAdmin();
        User savedUser = createUser();
        UserDTO dto = new UserDTO();

        UserService spyService = spy(userService);

        doReturn(savedUser)
                .when(spyService)
                .handleUser(request);

        given(modelMapper.map(savedUser, UserDTO.class))
                .willReturn(dto);

        UserDTO result = spyService.createUser(request);

        assertThat(result).isSameAs(dto);

        verify(spyService).handleUser(request);
        verify(modelMapper).map(savedUser, UserDTO.class);
    }

    // =========================================================
    // updateUser()
    // =========================================================

    @Test
    @DisplayName("updateUser - should map updated user to DTO")
    void updateUserShouldMapUpdatedUser() {

        UserRequestUpdate request = new UserRequestUpdate();
        User updatedUser = createUser();
        UserDTO dto = new UserDTO();

        UserService spyService = spy(userService);

        doReturn(updatedUser)
                .when(spyService)
                .handleUpdate(1L, request);

        given(modelMapper.map(updatedUser, UserDTO.class))
                .willReturn(dto);

        UserDTO result = spyService.updateUser(1L, request);

        assertThat(result).isSameAs(dto);

        verify(spyService).handleUpdate(1L, request);
        verify(modelMapper).map(updatedUser, UserDTO.class);
    }

    // =========================================================
    // getUserByID()
    // =========================================================

    @Test
    @DisplayName("getUserByID - should return DTO")
    void getUserByIdShouldReturnDto() {

        User user = createUser();
        UserDTO dto = new UserDTO();

        given(userRepository.existsByIdAndDeletedFalse(1L))
                .willReturn(true);

        given(userRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.of(user));

        given(modelMapper.map(user, UserDTO.class))
                .willReturn(dto);

        UserDTO result = userService.getUserByID(1L);

        assertThat(result).isSameAs(dto);
    }

    @Test
    @DisplayName("getUserByID - should throw when user does not exist")
    void getUserByIdShouldThrowWhenNotFound() {

        given(userRepository.existsByIdAndDeletedFalse(1L))
                .willReturn(false);

        assertThatThrownBy(
                () -> userService.getUserByID(1L)
        )
                .isInstanceOf(ResourceNotFoundException.class);

        verify(modelMapper, never())
                .map(any(), eq(UserDTO.class));
    }

    // =========================================================
    // getAllUser()
    // =========================================================

    @Test
    @DisplayName("getAllUser - should delegate to getPaginatedResumes")
    void getAllUserShouldDelegate() {

        ResponseDTO<List<UserDTO>> expected =
                new ResponseDTO<>(null, Collections.emptyList());

        UserService spyService = spy(userService);

        doReturn(expected)
                .when(spyService)
                .getPaginatedResumes(1, 10);

        ResponseDTO<List<UserDTO>> result =
                spyService.getAllUser(1, 10);

        assertThat(result).isSameAs(expected);

        verify(spyService)
                .getPaginatedResumes(1, 10);
    }

    // =========================================================
    // settingUser()
    // =========================================================

    @Test
    @DisplayName("settingUser - should return mapped setting")
    void settingUserShouldReturnMappedSetting() {

        UserSetting request = new UserSetting();
        User updatedUser = createUser();
        UserSetting expected = new UserSetting();

        UserService spyService = spy(userService);

        doReturn(updatedUser)
                .when(spyService)
                .handleSetting(1L, request);

        given(modelMapper.map(updatedUser, UserSetting.class))
                .willReturn(expected);

        UserSetting result = spyService.settingUser(1L, request);

        assertThat(result).isSameAs(expected);

        verify(spyService)
                .handleSetting(1L, request);
    }


    // =========================================================
    // getCurrentUser()
    // =========================================================

    @Nested
    @DisplayName("getCurrentUser()")
    class GetCurrentUserTests {
        private void prepareAuthentication(Long userId) {
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId.toString(),
                            null,
                            Collections.emptyList()
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);
        }

        @Test
        @DisplayName("ACTIVE - should return user")
        void activeShouldReturnUser() {

            User user = createUser();
            user.setStatus(UserStatus.ACTIVE);

            prepareAuthentication(1L);

            given(userRepository.findByIdAndDeletedFalse(1L))
                    .willReturn(Optional.of(user));

            User result = userService.getCurrentUser();

            assertThat(result).isSameAs(user);
        }

        @Test
        @DisplayName("BLOCKED - should throw")
        void blockedShouldThrow() {

            User user = createUser();
            user.setStatus(UserStatus.BLOCKED);

            prepareAuthentication(1L);

            given(userRepository.findByIdAndDeletedFalse(1L))
                    .willReturn(Optional.of(user));

            assertThatThrownBy(
                    () -> userService.getCurrentUser()
            )
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("blocked");
        }

        @Test
        @DisplayName("PENDING - should throw")
        void pendingShouldThrow() {

            User user = createUser();
            user.setStatus(UserStatus.PENDING);

            prepareAuthentication(1L);

            given(userRepository.findByIdAndDeletedFalse(1L))
                    .willReturn(Optional.of(user));

            assertThatThrownBy(
                    () -> userService.getCurrentUser()
            )
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("pending");
        }

        @Test
        @DisplayName("INACTIVE - should throw")
        void inactiveShouldThrow() {

            User user = createUser();
            user.setStatus(UserStatus.INACTIVE);

            prepareAuthentication(1L);

            given(userRepository.findByIdAndDeletedFalse(1L))
                    .willReturn(Optional.of(user));

            assertThatThrownBy(
                    () -> userService.getCurrentUser()
            )
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("deactivated");
        }
    }

    // =========================================================
    // updatePassword()
    // =========================================================

    @Nested
    @DisplayName("updatePassword()")
    class UpdatePasswordTests {

        @Test
        @DisplayName("Should update password successfully")
        void shouldUpdatePasswordSuccessfully() {

            User user = createUser();
            user.setPassword("old-encoded");

            PasswordRequest request = new PasswordRequest();
            request.setPassword("new-password");

            given(userRepository.findByIdAndDeletedFalse(1L))
                    .willReturn(Optional.of(user));

            given(passwordEncoder.matches(
                    "new-password",
                    "old-encoded"
            )).willReturn(false);

            given(passwordEncoder.encode("new-password"))
                    .willReturn("new-encoded");

            given(userRepository.save(user))
                    .willReturn(user);

            User result =
                    userService.updatePassword(1L, request);

            assertThat(result).isSameAs(user);
            assertThat(user.getPassword())
                    .isEqualTo("new-encoded");

            verify(passwordEncoder)
                    .encode("new-password");

            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw when new password equals current password")
        void shouldThrowWhenPasswordIsSame() {

            User user = createUser();
            user.setPassword("encoded-password");

            PasswordRequest request = new PasswordRequest();
            request.setPassword("same-password");

            given(userRepository.findByIdAndDeletedFalse(1L))
                    .willReturn(Optional.of(user));

            given(passwordEncoder.matches(
                    "same-password",
                    "encoded-password"
            )).willReturn(true);

            assertThatThrownBy(
                    () -> userService.updatePassword(1L, request)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("khác mật khẩu hiện tại");

            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // =========================================================
    // restoreUser()
    // =========================================================

    @Nested
    @DisplayName("restoreUser()")
    class RestoreUserTests {

        @Test
        @DisplayName("Should restore deleted user successfully")
        void shouldRestoreDeletedUserSuccessfully() {

            User user = createUser();

            user.setDeleted(true);
            user.setDeletedAt(
                    Instant.parse("2025-01-10T00:00:00Z")
            );
            user.setStatus(UserStatus.INACTIVE);
            user.setRefreshToken("old-token");

            UserDTO dto = new UserDTO();

            given(userRepository.findByIdAndDeletedTrue(1L))
                    .willReturn(Optional.of(user));

            given(userRepository.existsByEmailAndDeletedFalse(
                    user.getEmail()
            )).willReturn(false);

            given(userRepository.save(user))
                    .willReturn(user);

            given(modelMapper.map(user, UserDTO.class))
                    .willReturn(dto);

            UserDTO result =
                    userService.restoreUser(1L);

            assertThat(result).isSameAs(dto);

            assertThat(user.isDeleted()).isFalse();
            assertThat(user.getDeletedAt()).isNull();
            assertThat(user.getStatus())
                    .isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getRefreshToken())
                    .isNull();

            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw when deleted user does not exist")
        void shouldThrowWhenDeletedUserNotFound() {

            given(userRepository.findByIdAndDeletedTrue(1L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(
                    () -> userService.restoreUser(1L)
            )
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("User not found");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when email belongs to another active account")
        void shouldThrowWhenEmailAlreadyUsed() {

            User user = createUser();
            user.setDeleted(true);

            given(userRepository.findByIdAndDeletedTrue(1L))
                    .willReturn(Optional.of(user));

            given(userRepository.existsByEmailAndDeletedFalse(
                    user.getEmail()
            )).willReturn(true);

            assertThatThrownBy(
                    () -> userService.restoreUser(1L)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "Email đã được sử dụng bởi tài khoản khác"
                    );

            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================
    // deleteUserById()
    // =========================================================

    @Test
    @DisplayName("deleteUserById - should soft delete user")
    void deleteUserByIdShouldSoftDeleteUser() {

        User user = createUser();

        given(userRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.of(user));

        userService.deleteUserById(1L);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.isOnline()).isFalse();
        assertThat(user.getRefreshToken()).isNull();
        assertThat(user.getStatus())
                .isEqualTo(UserStatus.INACTIVE);

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("deleteUserById - should throw when user not found")
    void deleteUserByIdShouldThrowWhenNotFound() {

        given(userRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(
                () -> userService.deleteUserById(1L)
        )
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }
}

// Arrange

// Act

// Assert
