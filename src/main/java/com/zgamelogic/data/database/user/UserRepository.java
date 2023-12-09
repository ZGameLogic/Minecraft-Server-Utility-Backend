package com.zgamelogic.data.database.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    @Query(value = "SELECT u FROM User u WHERE u.token = :token and u.refreshToken = :refresh")
    Optional<User> findUserBySessionAndRefreshToken(@Param("token") String token, @Param("refresh") String refreshToken);
}
