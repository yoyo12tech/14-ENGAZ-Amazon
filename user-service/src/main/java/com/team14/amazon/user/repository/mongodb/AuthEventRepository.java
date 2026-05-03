package com.team14.amazon.user.repository.mongodb;

import com.team14.amazon.user.model.mongodb.AuthEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthEventRepository extends MongoRepository<AuthEvent, String> {
    Page<AuthEvent> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
}
