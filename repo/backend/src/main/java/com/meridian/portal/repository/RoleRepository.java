package com.meridian.portal.repository;

import com.meridian.portal.domain.Role;
import com.meridian.portal.domain.RoleName;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);

    Set<Role> findByNameIn(Set<RoleName> names);
}
