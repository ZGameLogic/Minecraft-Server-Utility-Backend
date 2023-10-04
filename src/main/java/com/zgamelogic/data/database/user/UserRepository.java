package com.zgamelogic.data.database.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    @Query(value = "SELECT * FROM \"msu_users\" u WHERE u.\"session_token\" = :token", nativeQuery = true)
    Optional<User> findUserBySessionToken(@Param("token") String token);
}
