package com.meridian.portal.discovery.repository;

import com.meridian.portal.discovery.domain.ZipReference;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ZipReferenceRepository extends JpaRepository<ZipReference, String> {

    List<ZipReference> findByZipCodeIn(Collection<String> zipCodes);

    @Query(value = """
        SELECT z.zip_code
        FROM zip_reference z
        WHERE (
            3959 * ACOS(
                LEAST(
                    1,
                    GREATEST(
                        -1,
                        COS(RADIANS(:lat)) * COS(RADIANS(z.latitude)) * COS(RADIANS(z.longitude) - RADIANS(:lon))
                        + SIN(RADIANS(:lat)) * SIN(RADIANS(z.latitude))
                    )
                )
            )
        ) <= :miles
        """, nativeQuery = true)
    List<String> findZipCodesWithinMiles(@Param("lat") double lat, @Param("lon") double lon, @Param("miles") int miles);
}
