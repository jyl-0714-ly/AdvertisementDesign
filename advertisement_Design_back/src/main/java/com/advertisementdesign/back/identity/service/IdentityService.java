package com.advertisementdesign.back.identity.service;

import com.advertisementdesign.back.common.exception.ApiErrorCode;
import com.advertisementdesign.back.common.exception.ApiException;
import com.advertisementdesign.back.identity.entity.UserEntity;
import com.advertisementdesign.back.identity.enums.UserRole;
import com.advertisementdesign.back.identity.enums.UserStatus;
import com.advertisementdesign.back.identity.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdentityService {
    private final UserMapper userMapper;

    public Optional<UserAccount> findByEmail(String email) {
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getEmail, email)
                .last("LIMIT 1"));
        return Optional.ofNullable(toAccount(user));
    }

    public Optional<UserProfile> findById(Long id) {
        return Optional.ofNullable(toProfile(userMapper.selectById(id)));
    }

    @Transactional
    public UserAccount createCustomer(String email, String passwordHash, String nickname) {
        LocalDateTime now = LocalDateTime.now();
        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash(passwordHash)
                .nickname(nickname)
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userMapper.insert(user);
        return requireAccount(user);
    }

    @Transactional
    public UserAccount updatePassword(Long id, String passwordHash) {
        UserEntity user = requireEntity(id);
        user.setPasswordHash(passwordHash);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return requireAccount(user);
    }

    @Transactional
    public UserAccount updateProfile(Long id, String nickname, String avatar, String phone) {
        UserEntity user = requireEntity(id);
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return requireAccount(user);
    }

    @Transactional
    public UserAccount recordLogin(Long id) {
        UserEntity user = requireEntity(id);
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        return requireAccount(user);
    }

    private UserEntity requireEntity(Long id) {
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        return user;
    }

    private UserAccount requireAccount(UserEntity user) {
        UserAccount account = toAccount(user);
        if (account == null) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        return account;
    }

    private UserAccount toAccount(UserEntity user) {
        if (user == null) {
            return null;
        }
        return new UserAccount(user.getId(), user.getEmail(), user.getPhone(), user.getPasswordHash(),
                user.getNickname(), user.getRole(), user.getAvatar(), user.getStatus());
    }

    private UserProfile toProfile(UserEntity user) {
        if (user == null) {
            return null;
        }
        return new UserProfile(
                user.getId(), user.getNickname(), user.getRole(), user.getAvatar(), user.getStatus());
    }

    public record UserProfile(Long id, String nickname, UserRole role, String avatar, UserStatus status) {
    }

    public record UserAccount(Long id, String email, String phone, String passwordHash,
                              String nickname, UserRole role, String avatar, UserStatus status) {
    }
}
