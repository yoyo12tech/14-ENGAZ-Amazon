package com.team14.amazon.user.repository;

import com.team14.amazon.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);

    @Query(value = "SELECT COUNT(*) FROM orders o WHERE o.user_id = :userId AND o.status NOT IN ('DELIVERED', 'CANCELLED', 'RETURNED')", nativeQuery = true)
    int countActiveOrdersForUser(@Param("userId") Long userId);

    @Query(value = "SELECT u.* FROM users u " +
            "JOIN orders o ON u.id = o.user_id " +
            "WHERE u.preferences ->> 'language' = :lang " +
            "GROUP BY u.id " +
            "HAVING COUNT(o.id) >= :minOrders",
            nativeQuery = true)
    List<User> findUsersByLanguageAndMinOrders(@Param("lang") String lang, @Param("minOrders") Integer minOrders);
}