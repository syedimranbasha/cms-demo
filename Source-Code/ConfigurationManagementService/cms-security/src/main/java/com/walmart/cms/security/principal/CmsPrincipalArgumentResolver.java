
package com.walmart.cms.security.principal;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

@Component
public class CmsPrincipalArgumentResolver
        implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == CmsPrincipal.class
                || parameter.getParameterType() == Optional.class
                && parameter.getGenericParameterType().getTypeName()
                .equals("java.util.Optional<com.walmart.cms.security.principal.CmsPrincipal>");
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        var authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {

            if (parameter.getParameterType() == Optional.class) {
                return Optional.empty();
            }

            throw new IllegalStateException(
                    "Authenticated JWT principal is not available");
        }

        String externalId = jwtAuth.getToken().getSubject();

        String username = jwtAuth.getToken()
                .getClaimAsString("preferred_username");

        if (username == null || username.isBlank()) {
            username = jwtAuth.getToken().getClaimAsString("email");
        }

        CmsPrincipal principal = new CmsPrincipal(externalId, username);

        if (parameter.getParameterType() == Optional.class) {
            return Optional.of(principal);
        }

        return principal;
    }
}
