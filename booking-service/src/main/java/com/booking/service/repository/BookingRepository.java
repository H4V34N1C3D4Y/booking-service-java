package com.booking.service.repository;

import com.booking.service.entity.Booking;
import com.booking.service.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA репозиторий для работы с бронированиями
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Найти бронирование по идентификатору запроса в Catalog Service
     * @param catalogRequestId идентификатор запроса
     * @return Optional с бронированием или empty
     */
    Optional<Booking> findByCatalogRequestId(UUID catalogRequestId);

    /**
     * Найти бронирования по фильтрам с пагинацией
     * Используется для получения списка бронирований с опциональными фильтрами
     *
     * @param userId идентификатор пользователя (опционально)
     * @param resourceId идентификатор ресурса (опционально)
     * @param status статус бронирования (опционально)
     * @param pageable параметры пагинации и сортировки
     * @return Page с результатами
     */
    @Query("SELECT b FROM Booking b WHERE " +
           "(:userId IS NULL OR b.userId = :userId) AND " +
           "(:resourceId IS NULL OR b.resourceId = :resourceId) AND " +
           "(:status IS NULL OR b.status = :status)")
    List<Booking> findByFilter(@Param("userId") Long userId,
                               @Param("resourceId") Long resourceId,
                               @Param("status") BookingStatus status,
                               Pageable pageable);

    /**
     * Получить только статус бронирования по ID
     * @param id идентификатор бронирования
     * @return статус бронирования или null
     */
    @Query("SELECT b.status FROM Booking b WHERE b.id = :id")
    BookingStatus findStatusById(@Param("id") Long id);

    @Query("""
        SELECT COUNT(b)
        FROM Booking b
        WHERE b.createdAt >= :from
          AND b.createdAt < :to
        """)
    long countByCreatedAtInRange(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
        SELECT b.resourceId, COUNT(b)
        FROM Booking b
        WHERE b.createdAt >= :from
          AND b.createdAt < :to
        GROUP BY b.resourceId
        ORDER BY COUNT(b) DESC
        
        """)
    List<Object[]> countTopResources(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable
    );

    @Query("""
        SELECT b.status, COUNT(b)
        FROM Booking b
        WHERE b.createdAt >= :from
          AND b.createdAt < :to
        GROUP BY b.status
        """)
    List<Object[]> countByStatus(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    List<Booking> findByStatusAndCancellationRequestedAtBefore(
            BookingStatus status,
            OffsetDateTime threshold
    );
}
