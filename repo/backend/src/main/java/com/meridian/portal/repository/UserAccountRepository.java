package com.meridian.portal.repository;

import com.meridian.portal.domain.RoleName;
import com.meridian.portal.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    long countDistinctByRoles_NameAndEnabledTrue(RoleName roleName);
}
