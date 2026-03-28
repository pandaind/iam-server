package com.vibes.iam.controller;

import com.vibes.iam.dto.CreateUserRequest;
import com.vibes.iam.dto.UpdateUserRequest;
import com.vibes.iam.dto.UserDto;
import com.vibes.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "User management APIs")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_USERS')")
    @Operation(summary = "Get all users", description = "Retrieve paginated list of all users")
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {
        Page<UserDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_USERS') or authentication.name == @userService.getUserById(#id).username")
    @Operation(summary = "Get user by ID", description = "Retrieve user details by ID")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasAuthority('READ_USERS') or authentication.name == #username")
    @Operation(summary = "Get user by username", description = "Retrieve user details by username")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        UserDto user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_USERS')")
    @Operation(summary = "Create user", description = "Create a new user")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request,
                                            Authentication authentication) {
        UserDto createdUser = userService.createUser(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE_USERS') or authentication.name == @userService.getUserById(#id).username")
    @Operation(summary = "Update user", description = "Update user details")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id,
                                            @Valid @RequestBody UpdateUserRequest request,
                                            Authentication authentication) {
        UserDto updatedUser = userService.updateUser(id, request, authentication.getName());
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_USERS')")
    @Operation(summary = "Delete user", description = "Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        userService.deleteUser(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @Operation(summary = "Enable user", description = "Enable a user account")
    public ResponseEntity<Void> enableUser(@PathVariable Long id, Authentication authentication) {
        userService.enableUser(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @Operation(summary = "Disable user", description = "Disable a user account")
    public ResponseEntity<Void> disableUser(@PathVariable Long id, Authentication authentication) {
        userService.disableUser(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @Operation(summary = "Unlock user", description = "Unlock a locked user account")
    public ResponseEntity<Void> unlockUser(@PathVariable Long id, Authentication authentication) {
        userService.unlockUser(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/role/{roleName}")
    @PreAuthorize("hasAuthority('READ_USERS')")
    @Operation(summary = "Get users by role", description = "Retrieve all users with specific role")
    public ResponseEntity<List<UserDto>> getUsersByRole(@PathVariable String roleName) {
        List<UserDto> users = userService.getUsersByRole(roleName);
        return ResponseEntity.ok(users);
    }
}