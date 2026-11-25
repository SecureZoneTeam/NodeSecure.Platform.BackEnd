package com.safezone.nodesecure.platform.iam.application.commands;

/** Public registration of a new owner together with a brand-new organization. */
public record SignUpCommand(String username, String email, String password, String fullName,
                            String organizationName) {
}
