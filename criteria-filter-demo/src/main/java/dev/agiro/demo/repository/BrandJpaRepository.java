package dev.agiro.demo.repository;

import dev.agiro.demo.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandJpaRepository extends JpaRepository<Brand, Long> {
}
