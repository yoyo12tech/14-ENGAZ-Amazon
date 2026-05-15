package com.team14.amazon.user.service;

import com.team14.amazon.contracts.dto.OrderSummaryDTO;
import com.team14.amazon.user.dto.*;
import com.team14.amazon.user.model.ShippingAddress;
import com.team14.amazon.user.model.User;
import com.team14.amazon.user.model.UserRole;
import com.team14.amazon.user.model.UserStatus;
import com.team14.amazon.user.repository.ShippingAddressRepository;
import com.team14.amazon.user.repository.UserRepository;
import com.team14.amazon.user.observer.EntityObserver;
import com.team14.amazon.user.security.JwtService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team14.amazon.user.repository.mongodb.AuthEventRepository;
import com.team14.amazon.user.model.mongodb.AuthEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.concurrent.TimeUnit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


import com.team14.amazon.contracts.feign.*;
import com.team14.amazon.contracts.dto.*;
import com.team14.amazon.contracts.events.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.team14.amazon.contracts.events.UserRegisteredEvent;


import java.math.BigDecimal;
import com.team14.amazon.contracts.events.UserDeactivatedEvent;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EntityObserver mongoEventLogger;
    private final EntityObserver redisCacheObserver;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final AuthEventRepository authEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final OrderServiceClient orderServiceClient;
    private final BillingServiceClient billingServiceClient;
    private final RabbitTemplate rabbitTemplate;

    public UserService(UserRepository userRepository,
                       ShippingAddressRepository shippingAddressRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EntityObserver mongoEventLogger,
                       EntityObserver redisCacheObserver,
                       AuthEventRepository authEventRepository,
                       StringRedisTemplate redisTemplate,
                       OrderServiceClient orderServiceClient,
                       BillingServiceClient billingServiceClient,
                       RabbitTemplate rabbitTemplate) {
        this.userRepository = userRepository;
        this.shippingAddressRepository = shippingAddressRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mongoEventLogger = mongoEventLogger;
        this.redisCacheObserver = redisCacheObserver;
        this.authEventRepository = authEventRepository;
        this.redisTemplate = redisTemplate;
        this.orderServiceClient = orderServiceClient;
        this.billingServiceClient = billingServiceClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void init() {
        register(mongoEventLogger);
        register(redisCacheObserver);
    }

    public void register(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregister(EntityObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    @Transactional
    public AuthResponseDTO registerUser(RegisterRequestDTO request) {
        // a) Validate blank fields
        if (isBlank(request.getName()) || isBlank(request.getEmail()) ||
            isBlank(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All fields are required and cannot be blank");
        }

        // b) Check unique email/phone
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() &&
            userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone is already registered");
        }

        // c, d) Create user with BCrypt password, CUSTOMER role, and ACTIVE status
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        user = userRepository.save(user);

        // e) Log REGISTERED event
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("userId", user.getId());
        eventPayload.put("timestamp", LocalDateTime.now());
        eventPayload.put("details", Map.of("email", user.getEmail()));
        notifyObservers("REGISTERED", eventPayload);

        // M3: Publish to RabbitMQ
        rabbitTemplate.convertAndSend("user.events", "user.registered",
                new UserRegisteredEvent(user.getId(), user.getEmail(), user.getRole().name()));

        // f) Generate token
        String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        
        return new AuthResponseDTO(token, jwtService.getExpirationMs());
    }

    @Transactional
    public AuthResponseDTO loginUser(LoginRequestDTO request) {
        // a) Find user by email — intentionally 401 (not 404) to prevent account enumeration
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // b) Verify password against stored BCrypt hash — 401 if mismatch
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // c) Log LOGGED_IN event to MongoDB via Observer
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("userId", user.getId());
        eventPayload.put("timestamp", LocalDateTime.now());
        eventPayload.put("details", Map.of("email", user.getEmail()));
        notifyObservers("LOGGED_IN", eventPayload);

        // d) Generate token with sub=email, uid=id, role=role
        String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        // e) Return token + expiration with 200
        return new AuthResponseDTO(token, jwtService.getExpirationMs());
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already in use.");
        }
        if (userRepository.findByPhone(user.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Phone number is already in use.");
        }

        // Default role on registration: CUSTOMER
        user.setRole(UserRole.CUSTOMER);

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User createdUser = userRepository.save(user);

        // Log USER_CREATED event
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("userId", createdUser.getId());
        eventPayload.put("timestamp", LocalDateTime.now());
        eventPayload.put("details", Map.of("email", createdUser.getEmail()));
        notifyObservers("USER_CREATED", eventPayload);

        return createdUser;
    }

    @Transactional
    public User updateUserRole(Long id, UserRole role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserRole oldRole = user.getRole();
        user.setRole(role);

        User updatedUser = userRepository.save(user);

        // Log ROLE_CHANGED event
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("userId", updatedUser.getId());
        eventPayload.put("timestamp", LocalDateTime.now());
        eventPayload.put("details", Map.of("oldRole", oldRole, "newRole", role));
        notifyObservers("ROLE_CHANGED", eventPayload);

        return updatedUser;
    }


    public Optional<User> updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(existingUser -> {

            if (!existingUser.getEmail().equals(updatedUser.getEmail()) &&
                    userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email is already in use.");
            }

            if (!existingUser.getPhone().equals(updatedUser.getPhone()) &&
                    userRepository.findByPhone(updatedUser.getPhone()).isPresent()) {
                throw new IllegalArgumentException("Phone number is already in use.");
            }

            existingUser.setName(updatedUser.getName());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setPhone(updatedUser.getPhone());
            existingUser.setRole(updatedUser.getRole());
            existingUser.setStatus(updatedUser.getStatus());

            existingUser.setPreferences(updatedUser.getPreferences());

            if (updatedUser.getPassword() != null && !updatedUser.getPassword().trim().isEmpty()) {
                // Hash the new password before saving
                existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }

            User savedUser = userRepository.save(existingUser);

            // Log USER_UPDATED event
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("userId", savedUser.getId());
            eventPayload.put("timestamp", LocalDateTime.now());
            eventPayload.put("details", Map.of("email", savedUser.getEmail()));
            notifyObservers("USER_UPDATED", eventPayload);

            return savedUser;
        });
    }

    public boolean deleteUser(Long id) {
        return userRepository.findById(id).map(user -> {
            userRepository.delete(user);
            
            // Log USER_DELETED event
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("userId", id);
            eventPayload.put("timestamp", LocalDateTime.now());
            eventPayload.put("details", Map.of("email", user.getEmail()));
            notifyObservers("USER_DELETED", eventPayload);
            
            return true;
        }).orElse(false);
    }

    public List<User> searchUsers(String name, String email, String role) {
        String normalizedName = normalize(name);
        String normalizedEmail = normalize(email);
        String normalizedRole = normalizeRole(role);

        return userRepository.searchUsers(normalizedName, normalizedEmail, normalizedRole);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }

        try {
            // validate role (IMPORTANT)
            return UserRole.valueOf(role.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role. Must be CUSTOMER or ADMIN.");
        }
    }

    public List<TopBuyerDTO> getTopBuyers(LocalDate startDate, LocalDate endDate, Integer limit) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }

        // M3: user-service cannot JOIN the transactions table.
        // 1. Fetch all users (or candidate pool)
        List<User> users = userRepository.findAll(); // Simplified candidate pool

        // 2. For each user, call Feign billing-service
        List<TopBuyerDTO> results = new ArrayList<>();
        String start = startDate.toString();
        String end = endDate.toString();

        for (User user : users) {
            try {
                BigDecimal totalSpent = billingServiceClient.getUserTransactionTotal(user.getId(), start, end);
                long orderCount = billingServiceClient.getUserOrderCount(user.getId(), start, end);
                if (totalSpent != null && totalSpent.compareTo(BigDecimal.ZERO) > 0) {
                    results.add(new TopBuyerDTO(user.getId(), user.getName(), totalSpent.doubleValue(), orderCount));
                }
            } catch (Exception e) {
                // Skip if billing service is unavailable for a user
            }
        }

        // 3. Sort and limit
        return results.stream()
                .sorted(Comparator.comparing(TopBuyerDTO::getTotalSpent).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<User> findUsersByPreference(String key, String value) {
        if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key and value must not be blank");
        }
        return userRepository.findAll().stream()
                .filter(user -> user.getPreferences() != null && value.equals(user.getPreferences().get(key)))
                .toList();
    }

    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        int activeOrdersCount = 0;
        try {
            // M3: Replace direct count query with Feign call
            activeOrdersCount = orderServiceClient.getActiveOrderCount(id);
        } catch (feign.FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order service temporarily unavailable");
        }

        if (activeOrdersCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has active orders");
        }

        user.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(user);

        // Log USER_DEACTIVATED event
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("userId", id);
        eventPayload.put("timestamp", LocalDateTime.now());
        eventPayload.put("details", Map.of("email", user.getEmail()));
        notifyObservers("USER_DEACTIVATED", eventPayload);

        // M3: Publish to RabbitMQ
        rabbitTemplate.convertAndSend("user.events", "user.deactivated", new UserDeactivatedEvent(id));
    }

    public UserProfileDTO getUserProfile(Long userId) {
        // 1. Find the user or throw 404
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 2. Map data to the DTO
        UserProfileDTO profileDTO = new UserProfileDTO();
        profileDTO.setUserId(user.getId());
        profileDTO.setName(user.getName());
        profileDTO.setEmail(user.getEmail());
        profileDTO.setPhone(user.getPhone());
        profileDTO.setPreferences(user.getPreferences());

        // 3. Attach the addresses and calculate the total count
        profileDTO.setShippingAddresses(user.getShippingAddresses());

        // Safe check in case the list is null
        int addressCount = (user.getShippingAddresses() != null) ? user.getShippingAddresses().size() : 0;
        profileDTO.setTotalAddresses(addressCount);

        return profileDTO;
    }

    @Transactional
    public User setDefaultShippingAddress(Long userId, Long addressId) {
        // 1. Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 2. Find the target shipping address
        ShippingAddress targetAddress = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipping address not found"));

        // 3. Verify the address belongs to this user
        if (!targetAddress.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address does not belong to this user");
        }

        // 4. Set isDefault = false on ALL of the user's existing addresses
        for (ShippingAddress address : user.getShippingAddresses()) {
            address.setDefault(false);
        }

        // 5. Set the target address to true
        targetAddress.setDefault(true);

        // 6. Save and return the updated user
        User savedUser = userRepository.save(user);

        // Log DEFAULT_ADDRESS_SET event
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("userId", userId);
        eventPayload.put("timestamp", LocalDateTime.now());
        eventPayload.put("details", Map.of("addressId", addressId));
        notifyObservers("DEFAULT_ADDRESS_SET", eventPayload);

        return savedUser;
    }

    public List<User> findUsersByLanguageAndMinOrders(String lang, Integer minOrders) {
        // Validate lang is not blank per requirement
        if (lang == null || lang.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Language cannot be blank");
        }
        
        // M3: Filter by language locally, then check order count via Feign
        List<User> matchingUsers = userRepository.findAll().stream()
                .filter(u -> u.getPreferences() != null && lang.equals(u.getPreferences().get("language")))
                .toList();

        return matchingUsers.stream()
                .filter(u -> {
                    try {
                        return orderServiceClient.getTotalOrderCount(u.getId()) >= minOrders;
                    } catch (feign.FeignException e) {
                        return false; // Skip user if order service is down
                    }
                })
                .collect(Collectors.toList());
    }

    public UserOrderSummaryDTO getUserOrderSummary(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            // M3: Replace cross-service SQL JOIN with Feign call
            OrderSummaryDTO orderSummary = orderServiceClient.getUserOrderSummary(id);

            return UserOrderSummaryDTO.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .totalOrders(orderSummary.getTotalOrders())
                    .completedOrders(orderSummary.getCompletedOrders())
                    .cancelledOrders(orderSummary.getCancelledOrders())
                    .totalSpent(orderSummary.getTotalSpent())
                    .averageOrderValue(orderSummary.getAverageOrderValue())
                    .build();
        } catch (feign.FeignException.NotFound e) {
            return UserOrderSummaryDTO.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .totalOrders(0L)
                    .completedOrders(0L)
                    .cancelledOrders(0L)
                    .totalSpent(0.0)
                    .averageOrderValue(0.0)
                    .build();
        } catch (feign.FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order service temporarily unavailable");
        }
    }

    public User updateUserPreferences(Long id, Map<String, Object> newPreferences) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getPreferences() == null) {
            user.setPreferences(new HashMap<>());
        }

        Map<String, Object> existingPreferences = user.getPreferences();

        for (Map.Entry<String, Object> entry : newPreferences.entrySet()) {
            existingPreferences.put(entry.getKey(), entry.getValue());
        }

        user.setPreferences(existingPreferences);

        User savedUser = userRepository.save(user);

        // Log USER_UPDATED event
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("userId", id);
        eventPayload.put("timestamp", LocalDateTime.now());
        eventPayload.put("details", Map.of("email", savedUser.getEmail()));
        notifyObservers("USER_UPDATED", eventPayload);

        return savedUser;
        }

    public ActivityFeedDTO getActivityFeed(Long id, Long callerUid, String callerRole,
                                           int page, int size) {
        // b) Ownership check
        boolean isOwner = callerUid != null && callerUid.equals(id);
        boolean isAdmin = "ADMIN".equals(callerRole);
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: you can only view your own activity feed");
        }

        // c) Verify user exists in PostgreSQL
        userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Cap size at 100
        int cappedSize = Math.min(size, 100);

        // e) Check Redis cache first
        String cacheKey = "user-service::S1-F12::" + id + "::page=" + page + "::size=" + cappedSize;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                return mapper.readValue(cached, ActivityFeedDTO.class);
            }
        } catch (Exception e) {
            // Redis soft dependency — continue if cache fails
        }

        // d) Query MongoDB paginated, sorted by most recent first
        Pageable pageable = PageRequest.of(page, cappedSize);
        Page<AuthEvent> eventPage = authEventRepository
                .findByUserIdOrderByTimestampDesc(id, pageable);

        List<ActivityFeedDTO.ActivityEventDTO> content = eventPage.getContent().stream()
                .map(event -> new ActivityFeedDTO.ActivityEventDTO(
                        event.getAction(),
                        event.getTimestamp(),
                        event.getDetails()))
                .collect(Collectors.toList());

        ActivityFeedDTO result = new ActivityFeedDTO(
                content,
                page,
                cappedSize,
                eventPage.getTotalElements());

        // e) Store in Redis as JSON string for 5 minutes
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String json = mapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Redis soft dependency — cache miss is acceptable
        }

        return result;
    }

    }





