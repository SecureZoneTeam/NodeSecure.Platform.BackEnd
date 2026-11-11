package com.safezone.nodesecure.platform.iam.application.commandservices;

public sealed interface UserCommandFailure permits
        UserCommandFailure.UsernameTaken,
        UserCommandFailure.EmailTaken,
        UserCommandFailure.InvalidCredentials,
        UserCommandFailure.NotFound {

    String messageKey();

    record UsernameTaken() implements UserCommandFailure {
        @Override
        public String messageKey() {
            return "iam.error.usernameTaken";
        }
    }

    record EmailTaken() implements UserCommandFailure {
        @Override
        public String messageKey() {
            return "iam.error.emailTaken";
        }
    }

    record InvalidCredentials() implements UserCommandFailure {
        @Override
        public String messageKey() {
            return "iam.error.invalidCredentials";
        }
    }

    record NotFound(Long id) implements UserCommandFailure {
        @Override
        public String messageKey() {
            return "iam.error.notFoundById";
        }
    }
}
