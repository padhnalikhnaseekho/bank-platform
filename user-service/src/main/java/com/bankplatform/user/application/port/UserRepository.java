package com.bankplatform.user.application.port;

import com.bankplatform.user.domain.Email;
import com.bankplatform.user.domain.User;
import com.bankplatform.user.domain.UserId;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);
}
