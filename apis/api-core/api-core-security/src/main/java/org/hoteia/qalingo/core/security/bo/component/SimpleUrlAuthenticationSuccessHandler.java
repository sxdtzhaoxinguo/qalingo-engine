/**
 * Most of the code in the Qalingo project is copyrighted Hoteia and licensed
 * under the Apache License Version 2.0 (release version 0.8.0)
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *                   Copyright (c) Hoteia, 2012-2014
 * http://www.hoteia.com - http://twitter.com/hoteia - contact@hoteia.com
 *
 */
package org.hoteia.qalingo.core.security.bo.component;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hoteia.qalingo.core.Constants;
import org.hoteia.qalingo.core.domain.User;
import org.hoteia.qalingo.core.domain.UserConnectionLog;
import org.hoteia.qalingo.core.domain.enumtype.BoUrls;
import org.hoteia.qalingo.core.security.RedirectStrategy;
import org.hoteia.qalingo.core.service.BackofficeUrlService;
import org.hoteia.qalingo.core.service.UserConnectionLogService;
import org.hoteia.qalingo.core.service.UserService;
import org.hoteia.qalingo.core.web.resolver.RequestData;
import org.hoteia.qalingo.core.web.util.RequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

@Component(value="simpleUrlAuthenticationSuccessHandler")
public class SimpleUrlAuthenticationSuccessHandler extends org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
    @Autowired
    protected UserService userService;

    @Autowired
    protected UserConnectionLogService userConnectionLogService;
    
	@Autowired
    protected RequestUtil requestUtil;
	
	@Autowired
    protected BackofficeUrlService backofficeUrlService;
	
	@Autowired
    protected RedirectStrategy redirectStrategy;

    @Autowired
    protected HttpSessionRequestCache requestCache;
	
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

    	// Find the current user
    	final User user = userService.getUserActivedByLoginOrEmail(authentication.getName());

    	// Persit only the new UserConnectionLog
    	UserConnectionLog userConnectionLog = new UserConnectionLog();
    	userConnectionLog.setUserId(user.getId());
    	userConnectionLog.setLoginDate(new Date());
    	userConnectionLog.setApp(Constants.APP_NAME_BO_BUSINESS_CODE);
    	userConnectionLog.setHost(requestUtil.getRemoteAddr(request));
    	
    	userConnectionLog.setPublicAddress(request.getHeader(Constants.X_FORWARDED_FOR));
        userConnectionLog.setPrivateAddress(request.getRemoteAddr());
    	
    	userConnectionLogService.saveOrUpdateUserConnectionLog(userConnectionLog);

		try {
	    	// Update the User in Session
	    	user.getConnectionLogs().add(userConnectionLog);
	    	requestUtil.updateCurrentUser(request, user);
	    	
            setUseReferer(false);
            String targetUrl = null;
            String savedRequestUrl = null;
            if(requestCache != null) {
                SavedRequest savedRequest = (SavedRequest) requestCache.getRequest(request, response);
                if(savedRequest != null) {
                    savedRequestUrl = savedRequest.getRedirectUrl();
                    // CLEAN CONTEXT FROM URL
                    savedRequestUrl = requestUtil.cleanUrlWebappContextPath(request, savedRequestUrl);
                }
            }
            
            String lastUrl = requestUtil.getCurrentRequestUrlNotSecurity(request);

            RequestData requestData = requestUtil.getRequestData(request);
            // SANITY CHECK
            if (StringUtils.isNotEmpty(savedRequestUrl)) {
                targetUrl = backofficeUrlService.cleanAbsoluteUrl(requestData, savedRequestUrl);;
            } else if (StringUtils.isNotEmpty(lastUrl)) {
                // && (lastUrl.contains("cart") || lastUrl.contains("checkout"))
                targetUrl = lastUrl;
            } else {
                targetUrl = backofficeUrlService.generateRedirectUrl(BoUrls.HOME, requestData);
            }

            setDefaultTargetUrl(targetUrl);
            redirectStrategy.sendRedirect(request, response, targetUrl);
	        
		} catch (Exception e) {
			logger.error("", e);
		}

    }

}