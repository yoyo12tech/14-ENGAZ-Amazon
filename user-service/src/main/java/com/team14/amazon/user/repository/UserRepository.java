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

    @Query(value = """
    SELECT * FROM users u
    WHERE (:name IS NULL OR u.name ILIKE CONCAT('%', :name, '%'))
      AND (:email IS NULL OR u.email ILIKE CONCAT('%', :email, '%'))
      AND (:role IS NULL OR u.role = :role)
""", nativeQuery = true)
List<User> searchUsers(@Param("name") String name,@Param("email") String email,@Param("role") String role);

@Query(value = """
    SELECT
        COUNT(o.id)                                                         AS totalOrders,
        COUNT(CASE WHEN o.status = 'DELIVERED'  THEN 1 END)                AS completedOrders,
        COUNT(CASE WHEN o.status = 'CANCELLED'  THEN 1 END)                AS cancelledOrders,
        COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.total_amount END), 0) AS totalSpent,
        COALESCE(AVG(CASE WHEN o.status = 'DELIVERED' THEN o.total_amount END), 0) AS averageOrderValue
    FROM orders o
    WHERE o.user_id = :userId
""", nativeQuery = true)
Object[] getOrderSummaryByUserId(@Param("userId") Long userId);
}


