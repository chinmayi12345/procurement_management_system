package com.infosys.procurementsystem.service.impl;

import com.infosys.procurementsystem.entity.Department;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.exception.BadRequestException;
import com.infosys.procurementsystem.exception.ResourceNotFoundException;
import com.infosys.procurementsystem.repository.DepartmentRepository;
import com.infosys.procurementsystem.repository.UserRepository;
import com.infosys.procurementsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.infosys.procurementsystem.enums.Role;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    // Use constructor injection with @Lazy for PasswordEncoder to avoid potential
    // circular reference
    public UserServiceImpl(UserRepository userRepository,
            DepartmentRepository departmentRepository,
            @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    @Override
    @Transactional
    public User createUser(User user) {

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        Department department = null;

        if (user.getDepartment() != null && user.getDepartment().getId() != null) {

            department = departmentRepository.findById(user.getDepartment().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + user.getDepartment().getId()));

            user.setDepartment(department);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);
        if (savedUser.getRole() == Role.ADMIN && department != null) {
            department.setAdmin(savedUser);
            departmentRepository.save(department);
        }
        return savedUser;
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);

        if (!user.getUsername().equals(userDetails.getUsername()) &&
                userRepository.existsByUsername(userDetails.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
        if (!user.getEmail().equals(userDetails.getEmail()) &&
                userRepository.existsByEmail(userDetails.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        if (userDetails.getDepartment() != null && userDetails.getDepartment().getId() != null) {
            Department dept = departmentRepository.findById(userDetails.getDepartment().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department not found with id: " + userDetails.getDepartment().getId()));
            user.setDepartment(dept);
        } else {
            user.setDepartment(null);
        }

        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setFullName(userDetails.getFullName());
        user.setRole(userDetails.getRole());
        user.setStatus(userDetails.getStatus());

        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }
}
