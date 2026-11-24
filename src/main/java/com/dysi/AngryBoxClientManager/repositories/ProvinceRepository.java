package com.dysi.AngryBoxClientManager.repositories;
import com.dysi.AngryBoxClientManager.models.Province;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProvinceRepository extends JpaRepository<Province, Long> {
    List<Province> findByRemoveDateIsNullOrderByNameAsc();
}
