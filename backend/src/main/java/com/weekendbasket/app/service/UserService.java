package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.UserDto.*;
import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.RoleAccess;
import com.weekendbasket.app.model.User;
import com.weekendbasket.app.model.UserProfile;
import com.weekendbasket.app.model.UserToken;
import com.weekendbasket.app.repository.RoleAccessRepository;
import com.weekendbasket.app.repository.RoleRepository;
import com.weekendbasket.app.repository.UserProfileRepository;
import com.weekendbasket.app.repository.UserRepository;
import com.weekendbasket.app.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final RoleAccessRepository roleAccessRepository;
    private final UserTokenRepository userTokenRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return buildResponse(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(String phoneNumber, UpdateProfileRequest request) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.username() != null && !request.username().isBlank()) {
            user.setUsername(request.username());
        }
        userRepository.save(user);

        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setEmail(request.email());
        profile.setFlatNumber(request.flatNumber());
        profile.setBlock(request.block());
        profileRepository.save(profile);

        return buildResponse(user);
    }

    @Transactional
    public void updateUsername(String phoneNumber, String username) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setUsername(username);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return buildResponse(user);
    }

    @Transactional
    public void blockUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setStatus("BLOCKED");
        userRepository.save(user);
    }

    @Transactional
    public void unblockUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setStatus("ACTIVE");
        userRepository.save(user);
    }

    @Transactional
    public void assignRole(Long userId, String roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        var role = roleRepository.findByRoleId(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        boolean alreadyAssigned = roleAccessRepository.findByUserId(userId)
                .stream()
                .anyMatch(ra -> ra.getRole().getRoleId().equals(roleId));

        if (alreadyAssigned) {
            throw new WeekendBasketException("Role already assigned to this user");
        }

        roleAccessRepository.save(RoleAccess.builder()
                .user(user)
                .role(role)
                .build());
    }

    @Transactional
    public void removeRole(Long userId, String roleId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        roleAccessRepository.deleteByUserIdAndRoleId(userId, roleId);
    }

    @Transactional
    public void saveToken(Long userId, String tokenHash, LocalDateTime issuedAt,
                          LocalDateTime expiredAt, String deviceHint) {
        User user = userRepository.findById(userId).orElseThrow();
        userTokenRepository.save(UserToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .issuedAt(issuedAt)
                .expiredAt(expiredAt)
                .lastUsedAt(issuedAt)
                .deviceHint(deviceHint)
                .build());
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return userTokenRepository.findActiveSessions(userId, now).stream()
                .map(t -> new SessionResponse(
                        t.getIssuedAt(),
                        t.getLastUsedAt(),
                        t.getExpiredAt(),
                        t.getDeviceHint(),
                        t.getLastUsedAt().isAfter(now.minusMinutes(5))))
                .toList();
    }

    private UserProfileResponse buildResponse(User user) {
        UserProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);

        List<String> roles = roleAccessRepository.findByUserId(user.getId())
                .stream()
                .map(ra -> ra.getRole().getRoleId())
                .toList();

        return new UserProfileResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getUsername(),
                user.getReferralCode(),
                user.getStatus(),
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null,
                profile != null ? profile.getEmail() : null,
                profile != null ? profile.getFlatNumber() : null,
                profile != null ? profile.getBlock() : null,
                roles
        );
    }
}
