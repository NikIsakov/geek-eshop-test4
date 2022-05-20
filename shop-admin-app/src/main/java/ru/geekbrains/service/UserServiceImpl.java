package ru.geekbrains.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.geekbrains.controller.dto.RoleDto;
import ru.geekbrains.controller.dto.UserDto;
import ru.geekbrains.controller.dto.UserListParams;
import ru.geekbrains.persist.RoleRepository;
import ru.geekbrains.persist.UserRepository;
import ru.geekbrains.persist.UserSpecifications;
import ru.geekbrains.persist.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;


    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
        /*
        закомментированный код ниже не давал зайти в админку через localhosts,
        видимо из-за не корректного типа данных. Код выше все исправил
         */
//        return new PasswordEncoder() {
//            @Override
//            public String encode(CharSequence rawPassword) {
//                return (String) rawPassword;
//                /* первоначально стояло значение return null, из-за этого выходила ошибка:
//                Error creating bean with name 'securityConfig': Injection of autowired dependencies failed;
//                 nested exception is java.lang.IllegalArgumentException: password cannot be null
//                 */
//            }
//
//            @Override
//            public boolean matches(CharSequence rawPassword, String encodedPassword) {
//                return false;
//            }
//        };
    }

    @Lazy
    /*
    Выходит ошибка:
    Description:
The dependencies of some of the beans in the application context form a cycle:
   userController defined in file [/Users/nikolajisakov/IdeaProjects/geek-eshop-test4/shop-admin-app/target/classes/ru/geekbrains/controller/UserController.class]┌─────┐
|  userServiceImpl defined in file [/Users/nikolajisakov/IdeaProjects/geek-eshop-test4/shop-admin-app/target/classes/ru/geekbrains/service/UserServiceImpl.class]└─────┘
Action:
Relying upon circular references is discouraged and they are prohibited by default.
Update your application to remove the dependency cycle between beans. As a last resort,
it may be possible to break the cycle automatically by setting spring.main.allow-circular-references to true.

аннотация @Lazy позволила убрать эту ошибку
     */
    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserDto> findAll() {
        return userRepository.findAll().stream()
                .map(user -> new UserDto(user.getId(),
                        user.getUsername()))
                .collect(Collectors.toList());
    }

    @Override
    public Page<UserDto> findWithFilter(UserListParams userListParams) {
        Specification<User> spec = Specification.where(null);

        if (userListParams.getUsernameFilter() != null && !userListParams.getUsernameFilter().isBlank()) {
            spec = spec.and(UserSpecifications.usernamePrefix(userListParams.getUsernameFilter()));
        }
        if (userListParams.getMinAge() != null) {
            spec = spec.and(UserSpecifications.minAge(userListParams.getMinAge()));
        }
        if (userListParams.getMaxAge() != null) {
            spec = spec.and(UserSpecifications.maxAge(userListParams.getMaxAge()));
        }

        return userRepository.findAll(spec,
                        PageRequest.of(
                                Optional.ofNullable(userListParams.getPage()).orElse(1) - 1,
                                Optional.ofNullable(userListParams.getSize()).orElse(3),
                                Sort.by(Optional.ofNullable(userListParams.getSortField())
                                        .filter(c -> !c.isBlank())
                                        .orElse("id"))))
                .map(user -> new UserDto(user.getId(), user.getUsername()));
    }

    @Override
    public Optional<UserDto> findById(Long id) {
        return userRepository.findById(id)
                .map(user -> new UserDto(user.getId(), user.getUsername(), user.getEmail(), mapRolesDto(user)));
    }

    @Override
    public void save(UserDto userDto) {
        User user = new User(
                userDto.getId(),
                userDto.getUsername(),
                userDto.getEmail(),
                passwordEncoder.encode(userDto.getPassword()),
                userDto.getRoles().stream()
                        .map(roleDto -> roleRepository.getById(roleDto.getId()))
                        .collect(Collectors.toSet()));
        userRepository.save(user);
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    private static Set<RoleDto> mapRolesDto(User user) {
        return user.getRoles().stream()
                .map(role -> new RoleDto(role.getId(), role.getName()))
                .collect(Collectors.toSet());
    }
}
