package com.bankplatform.user.application.port;

import com.bankplatform.user.domain.User;

public interface TokenIssuer {

    String issueAccessToken(User user);
}
