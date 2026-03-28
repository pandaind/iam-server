package com.vibes.iam.service;

import com.vibes.iam.dto.CreateUserRequest;
import com.vibes.iam.dto.UpdateUserRequest;
import com.vibes.iam.dto.UserDto;
import com.vibes.iam.entity.Role;
import com.vibes.iam.entity.User;
import com.vibes.iam.exception.ResourceNotFoundException;
import com.vibes.iam.exception.DuplicateResourceException;
import com.vibes.iam.repository.RoleRepository;
import com.vibes.iam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserDto::new);
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return new UserDto(user);
    }

    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return new UserDto(user);
    }

    public UserDto createUser(CreateUserRequest request, String currentUsername) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = request.getRoles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);

        auditService.logUserAction(null, currentUsername, "CREATE_USER", "USER", 
                "Created user: " + savedUser.getUsername(), null, null, true, null);

        return new UserDto(savedUser);
    }

    public UserDto updateUser(Long id, UpdateUserRequest request, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!user.getUsername().equals(request.getUsername()) && 
            userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoles() != null) {
            Set<Role> roles = request.getRoles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);

        auditService.logUserAction(null, currentUsername, "UPDATE_USER", "USER", 
                "Updated user: " + savedUser.getUsername(), null, null, true, null);

        return new UserDto(savedUser);
    }

    public void deleteUser(Long id, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        userRepository.delete(user);

        auditService.logUserAction(null, currentUsername, "DELETE_USER", "USER", 
                "Deleted user: " + user.getUsername(), null, null, true, null);
    }

    public void enableUser(Long id, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setEnabled(true);
        userRepository.save(user);

        auditService.logUserAction(null, currentUsername, "ENABLE_USER", "USER", 
                "Enabled user: " + user.getUsername(), null, null, true, null);
    }

    public void disableUser(Long id, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setEnabled(false);
        userRepository.save(user);

        auditService.logUserAction(null, currentUsername, "DISABLE_USER", "USER", 
                "Disabled user: " + user.getUsername(), null, null, true, null);
    }

    public void unlockUser(Long id, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setAccountNonLocked(true);
        user.setFailedLoginAttempts(0);
        user.setLockedAt(null);
        userRepository.save(user);

        auditService.logUserAction(null, currentUsername, "UNLOCK_USER", "USER", 
                "Unlocked user: " + user.getUsername(), null, null, true, null);
    }

    public List<UserDto> getUsersByRole(String roleName) {
        return userRepository.findByRoleName(roleName).stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
    }
}