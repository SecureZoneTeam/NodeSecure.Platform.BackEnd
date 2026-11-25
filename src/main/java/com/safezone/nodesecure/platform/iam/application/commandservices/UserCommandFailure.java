package com.safezone.nodesecure.platform.iam.application.commandservices;

public sealed interface UserCommandFailure permits
        UserCommandFailure.UsernameTaken,
        UserCommandFailure.EmailTaken,
        UserCommandFailure.InvalidCredentials,
        UserCommandFailure.AccountSuspended,
        UserCommandFailure.NotFound,
        UserCommandFailure.Forbidden,
        UserCommandFailure.Validation {

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

    record AccountSuspended() implements UserCommandFailure {
        @Override
        public String messageKey() {
            return "iam.error.accountSuspended";
        }
    }

    /** Also returned for users of another organization, so their existence is not revealed. */
    record NotFound(Long id) implements UserCommandFailure {
        @Override
        public String messageKey() {
            return "iam.error.notFoundById";
        }
    }

    /** The operation is not allowed on this user (owner, own account, ...). */
    record Forbidden(String messageKey) implements UserCommandFailure {
    }

    record Validation(String messageKey) implements UserCommandFailure {
    }
}
