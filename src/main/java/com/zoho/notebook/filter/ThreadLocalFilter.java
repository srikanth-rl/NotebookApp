package com.zoho.notebook.filter;

import com.adventnet.iam.IAMUtil;
import com.adventnet.iam.User;
import com.zoho.notebook.context.UserSessionContext;
import com.zoho.notebook.service.UserService;

import java.io.IOException;
import javax.servlet.*;
import java.util.logging.Logger;

public class ThreadLocalFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(ThreadLocalFilter.class.getName());
    private final UserService userService = new UserService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            User iamUser = IAMUtil.getCurrentUser();

            if (iamUser != null) {
                long zuid = iamUser.getZUID();

                // Validate IAM user exists in local DB
                try {
                    userService.ensureIamUserInLocalDb(
                        zuid,
                        iamUser.getDisplayName(),
                        iamUser.getTimezone()
                    );
                } catch (Exception e) {
                    LOGGER.severe("Failed to insert IAM user in local DB: " + e.getMessage());
                }

                UserSessionContext.set(
                    zuid,
                    iamUser.getDisplayName(),
                    iamUser.getTimezone(),
                    request.getRemoteAddr()
                );

                LOGGER.info("ThreadLocal context initialized for ZUID: " + zuid);
            } else {
                LOGGER.warning("IAMUtil.getCurrentUser() returned null!\n IAM Ticket - " + IAMUtil.getCurrentTicket());
            }

            chain.doFilter(request, response);

        } finally {
            // Always clear ThreadLocal to avoid memory leaks across requests
            UserSessionContext.remove();
        }
    }
}