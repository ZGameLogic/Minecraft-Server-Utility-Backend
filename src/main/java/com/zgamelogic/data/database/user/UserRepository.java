package com.zgamelogic.data.database.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, String> {
    @Query("""
            SELECT CASE WHEN COUNT(u) > 0
            THEN true ELSE false END
            FROM User u JOIN u.permissions p WHERE
            (u.id = :userId AND KEY(p) = 'General Permissions' AND VALUE(p) LIKE '%A%') OR
            (u.id = :userId AND KEY(p) = :server AND VALUE(p) LIKE %:permission%)
            """)
    boolean userHasPermission(@Param("userId") String userId, @Param("server") String server, @Param("permission") String permission);
}
