package com.meridian.portal.discovery.repository;

import com.meridian.portal.discovery.domain.SearchQueryEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchQueryEventRepository extends JpaRepository<SearchQueryEvent, Long> {

    @Query(value = """
        SELECT sq.query_text
        FROM search_query_events sq
        WHERE sq.query_text IS NOT NULL
          AND sq.query_text <> ''
          AND sq.created_at >= :fromTs
        GROUP BY sq.query_text
        ORDER BY COUNT(*) DESC, MAX(sq.created_at) DESC
        LIMIT :limitCount
        """, nativeQuery = true)
    List<String> findTrendingQueries(@Param("fromTs") Instant fromTs, @Param("limitCount") int limitCount);

    @Query(value = """
        SELECT sq.query_text
        FROM search_query_events sq
        WHERE sq.username = :username
          AND sq.query_text IS NOT NULL
          AND sq.query_text <> ''
        GROUP BY sq.query_text
        ORDER BY MAX(sq.created_at) DESC
        LIMIT :limitCount
        """, nativeQuery = true)
    List<String> findUserSearchHistory(@Param("username") String username, @Param("limitCount") int limitCount);

    @Query(value = """
        SELECT sq.query_text
        FROM search_query_events sq
        WHERE sq.username = :username
          AND sq.query_text IS NOT NULL
          AND sq.query_text <> ''
          AND LOWER(sq.query_text) LIKE CONCAT('%', LOWER(:q), '%')
        GROUP BY sq.query_text
        ORDER BY MAX(sq.created_at) DESC
        LIMIT :limitCount
        """, nativeQuery = true)
    List<String> findUserSearchHistoryMatching(@Param("username") String username, @Param("q") String q, @Param("limitCount") int limitCount);

    @Modifying
    @Query("delete from SearchQueryEvent e where e.username = :username")
    int deleteByUsername(@Param("username") String username);
}
