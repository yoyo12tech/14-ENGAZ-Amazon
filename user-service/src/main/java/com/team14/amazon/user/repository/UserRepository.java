package com.team14.amazon.user.repository;

import com.team14.amazon.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.team14.amazon.user.model.UserRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByRole(UserRole role);

    @Query(value = """
    SELECT * FROM users u
    WHERE (:name IS NULL OR u.name ILIKE CONCAT('%', :name, '%'))
      AND (:email IS NULL OR u.email ILIKE CONCAT('%', :email, '%'))
      AND (:role IS NULL OR CAST(u.role AS VARCHAR) = :role)
""", nativeQuery = true)
List<User> searchUsers(@Param("name") String name,@Param("email") String email,@Param("role") String role);
}