package com.travelapp.repository;

import com.travelapp.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DestinationRepository extends JpaRepository<Destination, Long> {

    @Query("SELECT d FROM Destination d WHERE " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(d.country) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(d.city) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(d.state) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(d.tags) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Destination> search(@Param("q") String query);

    @Query("SELECT d FROM Destination d WHERE " +
           "LOWER(d.city) LIKE LOWER(CONCAT('%', :city, '%')) OR " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :city, '%'))")
    List<Destination> findByCityOrNameContaining(@Param("city") String city);

    boolean existsByCityIgnoreCaseAndCountryIgnoreCase(String city, String country);
}
