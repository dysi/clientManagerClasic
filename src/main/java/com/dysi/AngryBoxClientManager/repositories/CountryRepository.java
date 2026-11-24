package com.dysi.AngryBoxClientManager.repositories;
import com.dysi.AngryBoxClientManager.models.Country;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CountryRepository extends JpaRepository<Country, Long> {
    List<Country> findByRemoveDateIsNullOrderByNameAsc();
}
