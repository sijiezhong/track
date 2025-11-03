package io.github.sijiezhong.track.repository;

import io.github.sijiezhong.track.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    List<User> findByAppId(Integer appId);
    Optional<User> findByUsername(String username);
}


