package com.bankplatform.user.application;

import com.bankplatform.common.error.NotFoundException;
import com.bankplatform.user.application.port.UserRepository;
import com.bankplatform.user.domain.User;
import com.bankplatform.user.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCurrentUserUseCase {

    private final UserRepository userRepository;

    public GetCurrentUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getById(UserId userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
