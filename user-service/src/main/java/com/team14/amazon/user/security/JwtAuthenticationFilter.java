   package com.team14.amazon.user.security;

   import com.team14.amazon.user.repository.UserRepository;
   import com.team14.amazon.user.security.handler.*;
   import jakarta.servlet.FilterChain;
   import jakarta.servlet.ServletException;
   import jakarta.servlet.http.HttpServletRequest;
   import jakarta.servlet.http.HttpServletResponse;
   import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
   import org.springframework.security.core.authority.SimpleGrantedAuthority;
   import org.springframework.security.core.context.SecurityContextHolder;
   import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
   import org.springframework.stereotype.Component;
   import org.springframework.web.filter.OncePerRequestFilter;

   import java.io.IOException;
   import java.util.Collections;

   @Component
   public class JwtAuthenticationFilter extends OncePerRequestFilter {

       private final JwtService jwtService;
       private final UserRepository userRepository;

       public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
           this.jwtService = jwtService;
           this.userRepository = userRepository;
       }

       @Override
       protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
               throws ServletException, IOException {

           String path = request.getServletPath();
           if (path.contains("/api/auth/register") || path.contains("/api/auth/login") || path.contains("/health")) {
               filterChain.doFilter(request, response);
               return;
           }

           AuthContext ctx = new AuthContext(request);
           
           // Build the Chain of Responsibility
           AuthHandler tokenExtraction = new TokenExtractionHandler();
           AuthHandler signatureValidation = new SignatureValidationHandler(jwtService);
           AuthHandler userLoader = new UserLoaderHandler(userRepository, jwtService);
           AuthHandler roleAuthorization = new RoleAuthorizationHandler();

           tokenExtraction.setNext(signatureValidation);
           signatureValidation.setNext(userLoader);
           userLoader.setNext(roleAuthorization);

           // Optional: set required role based on path if needed
           // CC-2: Requirement for ADMIN-only access to role management
           if (path.startsWith("/api/users") && path.endsWith("/role") && request.getMethod().equals("PUT")) {
               ctx.setRequiredRole("ADMIN");
           }

           tokenExtraction.handle(ctx);

           if (ctx.hasError()) {
               response.setStatus(ctx.getErrorStatus());
               response.getWriter().write(ctx.getErrorReason());
               return;
           }

           if (ctx.getUser() != null) {
               UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                       ctx.getUser(), null, 
                       Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + ctx.getUser().getRole().name()))
               );
               authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
               SecurityContextHolder.getContext().setAuthentication(authentication);
           }

           filterChain.doFilter(request, response);
       }
   }
