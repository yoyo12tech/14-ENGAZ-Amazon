package com.team14.amazon.user.repository;

import com.team14.amazon.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);

    @Query(value = "SELECT u.id, u.name, SUM(o.total_amount) as totalSpent, COUNT(o.id) as orderCount " +
                   "FROM users u JOIN orders o ON u.id = o.user_id " +
                   "WHERE o.status = 'DELIVERED' " +
                   "AND o.delivered_at >= :startDateTime AND o.delivered_at <= :endDateTime " +
                   "GROUP BY u.id, u.name " +
                   "ORDER BY totalSpent DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopBuyers(@Param("startDateTime") LocalDateTime startDateTime,
                                 @Param("endDateTime") LocalDateTime endDateTime,
                                 @Param("limit") Integer limit);
}
